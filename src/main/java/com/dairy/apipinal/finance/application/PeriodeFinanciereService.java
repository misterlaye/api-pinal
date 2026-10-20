package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.finance.api.ResultatFinancier.Avertissement;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.PrixAlimentManquant;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcul financier d'une période, source unique des indicateurs du tableau de bord financier
 * et du tableau de bord général. Requêtes groupées (une par source de données), indépendantes
 * du nombre de jours, d'animaux ou de rations.
 */
@Service
@Transactional(readOnly = true)
public class PeriodeFinanciereService {

    static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProductionQueries productionQueries;
    private final NutritionQueries nutritionQueries;
    private final PrixVenteLaitRepository prixVenteLaitRepository;
    private final ChargeExploitationService chargeExploitationService;
    private final TenantContext tenantContext;

    public PeriodeFinanciereService(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries,
            PrixVenteLaitRepository prixVenteLaitRepository,
            ChargeExploitationService chargeExploitationService,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
        this.prixVenteLaitRepository = prixVenteLaitRepository;
        this.chargeExploitationService = chargeExploitationService;
        this.tenantContext = tenantContext;
    }

    public ResultatFinancier calculer(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) {
            throw new InvalidRentabilityPeriodException("La période de calcul est invalide.");
        }

        List<Avertissement> avertissements = new ArrayList<>();

        // 1. Lait et chiffre d'affaires, jour par jour au prix applicable
        Map<LocalDate, BigDecimal> laitParJour = productionQueries.getDailyMilkProductionLitres(debut, fin);
        List<PrixVenteLait> prix = prixVenteLaitRepository.findAllOverlappingPeriod(
                tenantContext.currentTenantId(), tenantContext.currentExploitationId(), debut, fin);

        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal chiffreAffaires = BigDecimal.ZERO;
        List<LocalDate> joursSansPrix = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> jour : laitParJour.entrySet()) {
            if (jour.getValue().signum() <= 0) {
                continue;
            }
            volume = volume.add(jour.getValue());
            Optional<PrixVenteLait> prixDuJour = prixApplicable(prix, jour.getKey());
            if (prixDuJour.isPresent()) {
                chiffreAffaires = chiffreAffaires.add(jour.getValue().multiply(prixDuJour.get().getPrixParLitre()));
            } else {
                joursSansPrix.add(jour.getKey());
            }
        }
        if (volume.signum() == 0) {
            avertissements.add(new Avertissement("AUCUNE_TRAITE",
                    "Aucune traite enregistrée sur la période : volume et chiffre d'affaires nuls, coût par litre non calculable."));
        }
        if (!joursSansPrix.isEmpty()) {
            chiffreAffaires = null;
            avertissements.add(new Avertissement("PRIX_LAIT_MANQUANT",
                    "Prix de vente du lait manquant " + plage(joursSansPrix.getFirst(), joursSansPrix.getLast())
                            + " (" + joursSansPrix.size() + " jour(s) de production) : chiffre d'affaires et marge non calculables."));
        }

        // 2. Coût alimentaire (rations valorisées)
        ExploitationFeedCostReference feed = nutritionQueries.calculateTotalFeedCost(debut, fin.plusDays(1))
                .orElseThrow(() -> new IllegalStateException("Le coût alimentaire n'a pas pu être déterminé."));
        for (PrixAlimentManquant manquant : feed.prixManquants()) {
            avertissements.add(new Avertissement("PRIX_ALIMENT_MANQUANT",
                    "Prix manquant pour l'aliment « " + manquant.alimentNom() + " » "
                            + plage(manquant.premierJour(), manquant.dernierJour())
                            + " : coût alimentaire incomplet."));
        }

        // 3. Autres charges et indicateurs dérivés
        BigDecimal autresCharges = chargeExploitationService.sumAutresCharges(debut, fin.plusDays(1));
        BigDecimal coutAlimentation = feed.coutAlimentation();
        BigDecimal coutTotal = coutAlimentation == null ? null : coutAlimentation.add(autresCharges);
        BigDecimal marge = chiffreAffaires == null || coutTotal == null ? null : chiffreAffaires.subtract(coutTotal);
        BigDecimal prixMoyen = chiffreAffaires == null || volume.signum() == 0 ? null : chiffreAffaires.divide(volume, SCALE, ROUNDING);
        BigDecimal coutParLitre = coutTotal == null || volume.signum() == 0 ? null : coutTotal.divide(volume, SCALE, ROUNDING);
        BigDecimal tauxMarge = marge == null || chiffreAffaires.signum() == 0
                ? null
                : marge.multiply(BigDecimal.valueOf(100)).divide(chiffreAffaires, SCALE, ROUNDING);

        return new ResultatFinancier(
                debut,
                fin,
                scale(volume),
                scale(chiffreAffaires),
                prixMoyen,
                scale(coutAlimentation),
                scale(feed.coutConnu()),
                scale(autresCharges),
                scale(coutTotal),
                coutParLitre,
                scale(marge),
                tauxMarge,
                List.copyOf(avertissements)
        );
    }

    static Optional<PrixVenteLait> prixApplicable(List<PrixVenteLait> prix, LocalDate jour) {
        return prix.stream()
                .filter(p -> !p.getDateDebut().isAfter(jour) && (p.getDateFin() == null || !p.getDateFin().isBefore(jour)))
                .findFirst();
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, ROUNDING);
    }

    private static String plage(LocalDate premier, LocalDate dernier) {
        return premier.equals(dernier)
                ? "le " + premier.format(JOUR)
                : "du " + premier.format(JOUR) + " au " + dernier.format(JOUR);
    }
}
