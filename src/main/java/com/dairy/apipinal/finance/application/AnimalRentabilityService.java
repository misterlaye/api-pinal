package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.FeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Rentabilité alimentaire d'un animal sur une période : lait (en litres) valorisé au prix applicable
 * chaque jour, diminué du coût de ses rations.
 * <p>
 * Le résultat est vide si le coût alimentaire ne peut pas être déterminé pour toute la période
 * (jour sans ration) ; un jour de production sans prix de vente applicable est une erreur explicite.
 */
@Service
@Transactional(readOnly = true)
public class AnimalRentabilityService {

    private static final int MONEY_SCALE = 4;

    private final ProductionQueries productionQueries;
    private final NutritionQueries nutritionQueries;
    private final PrixVenteLaitRepository prixVenteLaitRepository;
    private final TenantContext tenantContext;

    public AnimalRentabilityService(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries,
            PrixVenteLaitRepository prixVenteLaitRepository,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
        this.prixVenteLaitRepository = prixVenteLaitRepository;
        this.tenantContext = tenantContext;
    }

    public Optional<AnimalRentabilityResult> calculate(
            UUID animalId,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) {

        validatePeriod(animalId, periodeDebut, periodeFin);

        LocalDate finExclusive = periodeFin.plusDays(1);

        FeedCostReference feedCost =
                nutritionQueries.calculateFeedCost(
                        animalId,
                        periodeDebut,
                        finExclusive
                ).orElse(null);

        if (feedCost == null) {
            return Optional.empty();
        }

        List<PrixVenteLait> prixPeriode = prixVenteLaitRepository.findAllOverlappingPeriod(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                periodeDebut,
                periodeFin
        );

        BigDecimal volumeLaitLitres = BigDecimal.ZERO;
        BigDecimal chiffreAffaires = BigDecimal.ZERO;

        for (LocalDate jour = periodeDebut; jour.isBefore(finExclusive); jour = jour.plusDays(1)) {

            OffsetDateTime debutJour = jour.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime finJour = jour.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

            BigDecimal productionLitres = productionQueries.getMilkProductionLitres(animalId, debutJour, finJour);

            if (productionLitres.signum() > 0) {
                LocalDate jourCourant = jour;
                PrixVenteLait prix = PeriodeFinanciereService.prixApplicable(prixPeriode, jourCourant)
                        .orElseThrow(() -> new IllegalStateException(
                                "Aucun prix de vente du lait applicable pour le " + jourCourant
                        ));

                volumeLaitLitres = volumeLaitLitres.add(productionLitres);
                chiffreAffaires = chiffreAffaires.add(
                        productionLitres
                                .multiply(prix.getPrixParLitre())
                                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                );
            }
        }

        BigDecimal coutAlimentation = feedCost.coutAlimentation();
        BigDecimal marge = chiffreAffaires.subtract(coutAlimentation);

        BigDecimal prixMoyenParLitre = null;
        BigDecimal coutAlimentationParLitre = null;

        if (volumeLaitLitres.signum() > 0) {
            prixMoyenParLitre = chiffreAffaires.divide(volumeLaitLitres, MONEY_SCALE, RoundingMode.HALF_UP);
            coutAlimentationParLitre = coutAlimentation.divide(volumeLaitLitres, MONEY_SCALE, RoundingMode.HALF_UP);
        }

        return Optional.of(
                new AnimalRentabilityResult(
                        animalId,
                        periodeDebut,
                        periodeFin,
                        volumeLaitLitres,
                        chiffreAffaires,
                        coutAlimentation,
                        marge,
                        prixMoyenParLitre,
                        coutAlimentationParLitre,
                        marge.signum() > 0
                )
        );
    }

    private void validatePeriod(
            UUID animalId,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) {
        if (animalId == null) {
            throw new InvalidRentabilityPeriodException(
                    "L'identifiant de l'animal est obligatoire."
            );
        }

        if (periodeDebut == null || periodeFin == null) {
            throw new InvalidRentabilityPeriodException(
                    "La période est obligatoire."
            );
        }

        if (periodeFin.isBefore(periodeDebut)) {
            throw new InvalidRentabilityPeriodException(
                    "La date de fin doit être supérieure ou égale à la date de début."
            );
        }
    }
}
