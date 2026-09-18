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
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnimalRentabilityService {

    private static final int MONEY_SCALE = 4;

    private final ProductionQueries productionQueries;
    private final NutritionQueries nutritionQueries;
    private final PrixVenteLaitRepository prixVenteLaitRepository;
    private final MilkVolumeConversionPolicy milkVolumeConversionPolicy;
    private final TenantContext tenantContext;

    public AnimalRentabilityService(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries,
            PrixVenteLaitRepository prixVenteLaitRepository,
            MilkVolumeConversionPolicy milkVolumeConversionPolicy,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
        this.prixVenteLaitRepository = prixVenteLaitRepository;
        this.milkVolumeConversionPolicy = milkVolumeConversionPolicy;
        this.tenantContext = tenantContext;
    }

    public Optional<AnimalRentabilityResult> calculate(
            UUID animalId,
            LocalDate periodeDebut,
            LocalDate periodeFin
    ) {

        validatePeriod(animalId, periodeDebut, periodeFin);

        UUID tenantId = tenantContext.currentTenantId();
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

        BigDecimal volumeLaitKg = BigDecimal.ZERO;
        BigDecimal volumeLaitLitres = BigDecimal.ZERO;
        BigDecimal chiffreAffaires = BigDecimal.ZERO;

        LocalDate jour = periodeDebut;

        while (jour.isBefore(finExclusive)) {

            OffsetDateTime debutJour =
                    jour.atStartOfDay().atOffset(ZoneOffset.UTC);

            OffsetDateTime finJour =
                    jour.plusDays(1)
                            .atStartOfDay()
                            .atOffset(ZoneOffset.UTC);

            BigDecimal productionKg =
                    productionQueries.getMilkProductionKg(
                            animalId,
                            debutJour,
                            finJour
                    );

            if (productionKg.signum() > 0) {

                BigDecimal productionLitres =
                        milkVolumeConversionPolicy.convertKgToLitres(
                                productionKg
                        );

                LocalDate jourCourant = jour;

                PrixVenteLait prix =
                        prixVenteLaitRepository
                                .findApplicablePrice(tenantId, jourCourant)
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Aucun prix de vente du lait applicable pour le "
                                                        + jourCourant
                                        )
                                );

                BigDecimal revenuJour =
                        productionLitres
                                .multiply(prix.getPrixParLitre())
                                .setScale(
                                        MONEY_SCALE,
                                        RoundingMode.HALF_UP
                                );

                volumeLaitKg =
                        volumeLaitKg.add(productionKg);

                volumeLaitLitres =
                        volumeLaitLitres.add(productionLitres);

                chiffreAffaires =
                        chiffreAffaires.add(revenuJour);
            }

            jour = jour.plusDays(1);
        }

        BigDecimal coutAlimentation =
                feedCost.coutAlimentation();

        BigDecimal marge =
                chiffreAffaires.subtract(coutAlimentation);

        BigDecimal prixMoyenParLitre = null;
        BigDecimal coutAlimentationParLitre = null;

        if (volumeLaitLitres.signum() > 0) {

            prixMoyenParLitre =
                    chiffreAffaires.divide(
                            volumeLaitLitres,
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );

            coutAlimentationParLitre =
                    coutAlimentation.divide(
                            volumeLaitLitres,
                            MONEY_SCALE,
                            RoundingMode.HALF_UP
                    );
        }

        return Optional.of(
                new AnimalRentabilityResult(
                        animalId,
                        periodeDebut,
                        periodeFin,
                        volumeLaitKg,
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