package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.application.AnimalRentabilityResult;
import com.dairy.apipinal.finance.application.AnimalRentabilityService;
import com.dairy.apipinal.finance.application.MilkVolumeConversionPolicy;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.FeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalRentabilityServiceTest {

    @Mock
    private ProductionQueries productionQueries;

    @Mock
    private NutritionQueries nutritionQueries;

    @Mock
    private PrixVenteLaitRepository prixVenteLaitRepository;

    @Mock
    private MilkVolumeConversionPolicy milkVolumeConversionPolicy;

    @Mock
    private TenantContext tenantContext;

    private AnimalRentabilityService service;

    @BeforeEach
    void setUp() {
        service = new AnimalRentabilityService(
                productionQueries,
                nutritionQueries,
                prixVenteLaitRepository,
                milkVolumeConversionPolicy,
                tenantContext
        );
    }

    @Test
    void shouldCalculateAnimalRentabilityWithHistoricalMilkPrices() {

        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        LocalDate jour1 = LocalDate.of(2026, 9, 1);
        LocalDate jour2 = LocalDate.of(2026, 9, 2);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(
                nutritionQueries.calculateFeedCost(
                        animalId,
                        jour1,
                        jour2.plusDays(1)
                )
        ).thenReturn(
                Optional.of(
                        new FeedCostReference(
                                animalId,
                                jour1,
                                jour2.plusDays(1),
                                new BigDecimal("5000.00")
                        )
                )
        );

        when(
                productionQueries.getMilkProductionKg(
                        animalId,
                        dayStart(jour1),
                        dayStart(jour2)
                )
        ).thenReturn(new BigDecimal("10.000"));

        when(
                productionQueries.getMilkProductionKg(
                        animalId,
                        dayStart(jour2),
                        dayStart(jour2.plusDays(1))
                )
        ).thenReturn(new BigDecimal("20.000"));

        when(
                milkVolumeConversionPolicy.convertKgToLitres(
                        new BigDecimal("10.000")
                )
        ).thenReturn(new BigDecimal("10.000"));

        when(
                milkVolumeConversionPolicy.convertKgToLitres(
                        new BigDecimal("20.000")
                )
        ).thenReturn(new BigDecimal("20.000"));

        PrixVenteLait prixJour1 =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("600.00"),
                        jour1,
                        jour1
                );

        PrixVenteLait prixJour2 =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("650.00"),
                        jour2,
                        jour2
                );

        when(
                prixVenteLaitRepository.findApplicablePrice(
                        tenantId,
                        jour1
                )
        ).thenReturn(Optional.of(prixJour1));

        when(
                prixVenteLaitRepository.findApplicablePrice(
                        tenantId,
                        jour2
                )
        ).thenReturn(Optional.of(prixJour2));

        Optional<AnimalRentabilityResult> result =
                service.calculate(
                        animalId,
                        jour1,
                        jour2
                );

        assertThat(result)
                .isPresent();

        AnimalRentabilityResult rentability =
                result.orElseThrow();

        assertThat(rentability.volumeLaitKg())
                .isEqualByComparingTo("30.000");

        assertThat(rentability.volumeLaitLitres())
                .isEqualByComparingTo("30.000");

        assertThat(rentability.chiffreAffaires())
                .isEqualByComparingTo("19000.0000");

        assertThat(rentability.coutAlimentation())
                .isEqualByComparingTo("5000.00");

        assertThat(rentability.marge())
                .isEqualByComparingTo("14000.0000");

        assertThat(rentability.prixMoyenParLitre())
                .isEqualByComparingTo("633.3333");

        assertThat(rentability.coutAlimentationParLitre())
                .isEqualByComparingTo("166.6667");

        assertThat(rentability.rentable())
                .isTrue();
    }

    @Test
    void shouldReturnEmptyWhenFeedCostCannotBeCalculated() {

        UUID animalId = UUID.randomUUID();

        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 2);

        when(
                nutritionQueries.calculateFeedCost(
                        animalId,
                        debut,
                        fin.plusDays(1)
                )
        ).thenReturn(Optional.empty());

        Optional<AnimalRentabilityResult> result =
                service.calculate(
                        animalId,
                        debut,
                        fin
                );

        assertThat(result)
                .isEmpty();

        verifyNoInteractions(productionQueries);
        verifyNoInteractions(prixVenteLaitRepository);
        verifyNoInteractions(milkVolumeConversionPolicy);
    }

    @Test
    void shouldCalculateNegativeMarginWhenAnimalHasFeedCostButNoMilkProduction() {

        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        LocalDate jour = LocalDate.of(2026, 9, 1);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(
                nutritionQueries.calculateFeedCost(
                        animalId,
                        jour,
                        jour.plusDays(1)
                )
        ).thenReturn(
                Optional.of(
                        new FeedCostReference(
                                animalId,
                                jour,
                                jour.plusDays(1),
                                new BigDecimal("500.00")
                        )
                )
        );

        when(
                productionQueries.getMilkProductionKg(
                        animalId,
                        dayStart(jour),
                        dayStart(jour.plusDays(1))
                )
        ).thenReturn(BigDecimal.ZERO);

        Optional<AnimalRentabilityResult> result =
                service.calculate(
                        animalId,
                        jour,
                        jour
                );

        assertThat(result)
                .isPresent();

        assertThat(result.orElseThrow().chiffreAffaires())
                .isZero();

        assertThat(result.orElseThrow().marge())
                .isEqualByComparingTo("-500.00");

        assertThat(result.orElseThrow().rentable())
                .isFalse();

        verifyNoInteractions(prixVenteLaitRepository);
        verifyNoInteractions(milkVolumeConversionPolicy);
    }

    private static OffsetDateTime dayStart(LocalDate date) {
        return date
                .atStartOfDay()
                .atOffset(ZoneOffset.UTC);
    }
}