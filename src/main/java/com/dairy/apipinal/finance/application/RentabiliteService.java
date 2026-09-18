package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional
public class RentabiliteService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ProductionQueries productionQueries;
    private final NutritionQueries nutritionQueries;
    private final PrixVenteLaitRepository prixVenteLaitRepository;
    private final ChargeExploitationService chargeExploitationService;
    private final CalculRentabiliteRepository calculRepository;
    private final MilkVolumeConversionPolicy conversionPolicy;
    private final TenantContext tenantContext;

    public RentabiliteService(
            ProductionQueries productionQueries,
            NutritionQueries nutritionQueries,
            PrixVenteLaitRepository prixVenteLaitRepository,
            ChargeExploitationService chargeExploitationService,
            CalculRentabiliteRepository calculRepository,
            MilkVolumeConversionPolicy conversionPolicy,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.nutritionQueries = nutritionQueries;
        this.prixVenteLaitRepository = prixVenteLaitRepository;
        this.chargeExploitationService = chargeExploitationService;
        this.calculRepository = calculRepository;
        this.conversionPolicy = conversionPolicy;
        this.tenantContext = tenantContext;
    }

    public CalculRentabilite calculate(
            CalculateRentabilite command
    ) {
        validatePeriod(
                command.periodeDebut(),
                command.periodeFin()
        );

        UUID tenantId =
                tenantContext.currentTenantId();

        LocalDate finExclusive =
                command.periodeFin().plusDays(1);

        BigDecimal volumeLait =
                ZERO;

        BigDecimal chiffreAffaires =
                ZERO;

        LocalDate date =
                command.periodeDebut();

        while (date.isBefore(finExclusive)) {

            LocalDate lendemain =
                    date.plusDays(1);

            BigDecimal productionKg =
                    productionQueries.getTotalMilkProductionKg(
                            date.atStartOfDay().atOffset(ZoneOffset.UTC),
                            lendemain.atStartOfDay().atOffset(ZoneOffset.UTC)
                    );

            if (productionKg.signum() > 0) {

                BigDecimal productionLitres =
                        conversionPolicy.convertKgToLitres(
                                productionKg
                        );

                volumeLait =
                        volumeLait.add(productionLitres);

                /*
                 * date est modifiée à la fin de chaque itération.
                 * On utilise donc une variable locale effectivement finale
                 * pour pouvoir l'utiliser dans le lambda de orElseThrow().
                 */
                LocalDate dateCourante =
                        date;

                var prix =
                        prixVenteLaitRepository
                                .findApplicablePrice(
                                        tenantId,
                                        dateCourante
                                )
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Aucun prix de vente du lait "
                                                        + "applicable pour le "
                                                        + dateCourante
                                        )
                                );

                chiffreAffaires =
                        chiffreAffaires.add(
                                productionLitres.multiply(
                                        prix.getPrixParLitre()
                                )
                        );
            }

            date =
                    lendemain;
        }

        ExploitationFeedCostReference feedCost =
                nutritionQueries
                        .calculateTotalFeedCost(
                                command.periodeDebut(),
                                finExclusive
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Le coût alimentaire n'a pas pu être déterminé."
                                )
                        );

        BigDecimal autresCharges =
                chargeExploitationService.sumAutresCharges(
                        command.periodeDebut(),
                        finExclusive
                );

        CalculRentabilite calcul =
                CalculRentabilite.calculate(
                        tenantId,
                        OffsetDateTime.now(ZoneOffset.UTC),
                        command.periodeDebut(),
                        command.periodeFin(),
                        volumeLait,
                        chiffreAffaires,
                        feedCost.coutAlimentation(),
                        autresCharges
                );

        return calculRepository.save(calcul);
    }

    private void validatePeriod(
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException(
                    "Les bornes de la période sont obligatoires."
            );
        }

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La période de calcul est invalide."
            );
        }
    }
}