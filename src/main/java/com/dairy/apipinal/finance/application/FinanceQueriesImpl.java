package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.api.AnimalRentabilityReference;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.RentabiliteReference;
import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.domain.ChargeExploitation;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.finance.infrastructure.persistence.ChargeExploitationRepository;
import com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse;
import com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse.ChargesRepartition;
import com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse.Historique;
import com.dairy.apipinal.finance.infrastructure.web.TransactionResponse;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinanceQueriesImpl implements FinanceQueries {

    private static final Locale FR = Locale.FRANCE;
    private static final String ALIMENTATION = "ALIMENTATION";

    private final AnimalRentabilityService animalRentabilityService;
    private final PeriodeFinanciereService periodeFinanciereService;
    private final CalculRentabiliteRepository calculRentabiliteRepository;
    private final ChargeExploitationRepository chargeRepository;
    private final TenantContext tenantContext;

    public FinanceQueriesImpl(
            AnimalRentabilityService animalRentabilityService,
            PeriodeFinanciereService periodeFinanciereService,
            CalculRentabiliteRepository calculRentabiliteRepository,
            ChargeExploitationRepository chargeRepository,
            TenantContext tenantContext
    ) {
        this.animalRentabilityService = animalRentabilityService;
        this.periodeFinanciereService = periodeFinanciereService;
        this.calculRentabiliteRepository = calculRentabiliteRepository;
        this.chargeRepository = chargeRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public Optional<AnimalRentabilityReference> calculateAnimalRentability(
            UUID animalId,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        return animalRentabilityService
                .calculate(animalId, dateDebut, dateFin)
                .map(this::toAnimalReference);
    }

    @Override
    public Optional<RentabiliteReference> findLatestRentabilite() {

        return calculRentabiliteRepository
                .findFirstByTenantIdAndExploitationIdOrderByDateCalculDesc(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
                .map(this::toRentabiliteReference);
    }

    @Override
    public ResultatFinancier calculerPeriode(LocalDate debut, LocalDate fin) {
        return periodeFinanciereService.calculer(debut, fin);
    }

    @Override
    public FinanceDashboardResponse getDashboardSummary() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        // 1. Mois en cours : du 1er à aujourd'hui
        ResultatFinancier moisEnCours = periodeFinanciereService.calculer(currentMonth.atDay(1), today);

        // 2. Historique : trois mois précédents complets + mois en cours, mêmes règles de calcul
        List<String> labels = new ArrayList<>();
        List<BigDecimal> chiffreAffaires = new ArrayList<>();
        List<BigDecimal> coutTotal = new ArrayList<>();
        for (int i = 3; i >= 1; i--) {
            YearMonth mois = currentMonth.minusMonths(i);
            ResultatFinancier resultat = periodeFinanciereService.calculer(mois.atDay(1), mois.atEndOfMonth());
            labels.add(libelle(mois));
            chiffreAffaires.add(resultat.chiffreAffaires());
            coutTotal.add(resultat.coutTotal());
        }
        labels.add(libelle(currentMonth) + " (en cours)");
        chiffreAffaires.add(moisEnCours.chiffreAffaires());
        coutTotal.add(moisEnCours.coutTotal());

        // 3. Charges saisies du mois et répartition des coûts
        List<ChargeExploitation> charges = chargeRepository
                .findAllByTenantIdAndExploitationIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDesc(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        currentMonth.atDay(1),
                        today.plusDays(1)
                );

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<TransactionResponse> transactions = charges.stream()
                .map(c -> new TransactionResponse(
                        c.getId().toString(), "CHARGE", c.getCategorie(), c.getMontant().doubleValue(),
                        c.getDate().format(fmt), "COMPLETED", c.getLibelle()))
                .toList();

        return new FinanceDashboardResponse(
                moisEnCours,
                new Historique(labels, chiffreAffaires, coutTotal),
                repartition(moisEnCours, charges),
                transactions
        );
    }

    /**
     * Coût alimentaire valorisé par les rations, puis autres charges regroupées par catégorie.
     * Les charges de catégorie ALIMENTATION (achats) ne sont pas ajoutées : la consommation est déjà valorisée.
     */
    private static ChargesRepartition repartition(ResultatFinancier mois, List<ChargeExploitation> charges) {
        Map<String, BigDecimal> parCategorie = new LinkedHashMap<>();
        BigDecimal alimentation = mois.coutAlimentation() != null ? mois.coutAlimentation() : mois.coutAlimentationConnu();
        if (alimentation != null && alimentation.signum() > 0) {
            parCategorie.put("Alimentation (rations)", alimentation);
        }
        for (ChargeExploitation charge : charges) {
            if (ALIMENTATION.equalsIgnoreCase(charge.getCategorie())) {
                continue;
            }
            parCategorie.merge(libelleCategorie(charge.getCategorie()), charge.getMontant(), BigDecimal::add);
        }
        return new ChargesRepartition(
                List.copyOf(parCategorie.keySet()),
                parCategorie.values().stream().map(v -> v.setScale(PeriodeFinanciereService.SCALE, java.math.RoundingMode.HALF_UP)).toList(),
                mois.coutAlimentation() == null
        );
    }

    private static String libelle(YearMonth mois) {
        String nom = mois.getMonth().getDisplayName(TextStyle.SHORT, FR);
        return nom.substring(0, 1).toUpperCase(FR) + nom.substring(1) + " " + mois.getYear();
    }

    private static String libelleCategorie(String categorie) {
        if (categorie == null || categorie.isBlank()) {
            return "Autres";
        }
        String c = categorie.trim().toLowerCase(FR);
        return c.substring(0, 1).toUpperCase(FR) + c.substring(1);
    }

    private AnimalRentabilityReference toAnimalReference(
            AnimalRentabilityResult result
    ) {
        return new AnimalRentabilityReference(
                result.animalId(),
                result.dateDebut(),
                result.dateFin(),
                result.volumeLaitLitres(),
                result.chiffreAffaires(),
                result.coutAlimentation(),
                result.marge(),
                result.prixMoyenParLitre(),
                result.coutAlimentationParLitre(),
                result.rentable()
        );
    }

    private RentabiliteReference toRentabiliteReference(
            CalculRentabilite calcul
    ) {
        return new RentabiliteReference(
                calcul.getId(),
                calcul.getPeriodeDebut(),
                calcul.getPeriodeFin(),
                calcul.getDateCalcul(),
                calcul.getVolumeLait(),
                calcul.getChiffreAffaires(),
                calcul.getCoutAlimentation(),
                calcul.getAutresCharges(),
                calcul.getCoutTotal(),
                calcul.getCoutRevientParLitre(),
                calcul.getMarge()
        );
    }
}
