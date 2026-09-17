package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.application.CalculateRentabilite;
import com.dairy.apipinal.finance.application.ChargeExploitationService;
import com.dairy.apipinal.finance.application.MilkVolumeConversionPolicy;
import com.dairy.apipinal.finance.application.RentabiliteService;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentabiliteServiceTest {

    @Mock
    private ProductionQueries productionQueries;

    @Mock
    private NutritionQueries nutritionQueries;

    @Mock
    private PrixVenteLaitRepository prixVenteLaitRepository;

    @Mock
    private ChargeExploitationService chargeExploitationService;

    @Mock
    private CalculRentabiliteRepository calculRepository;

    @Mock
    private MilkVolumeConversionPolicy conversionPolicy;

    @Mock
    private TenantContext tenantContext;

    @Test
    void shouldCalculateAndPersistRentability() {

        UUID tenantId = UUID.randomUUID();

        LocalDate jour1 =
                LocalDate.of(2026, 9, 1);

        LocalDate jour2 =
                LocalDate.of(2026, 9, 2);

        LocalDate fin =
                LocalDate.of(2026, 9, 2);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(productionQueries.getTotalMilkProductionKg(
                jour1.atStartOfDay().atOffset(ZoneOffset.UTC),
                jour1.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        )).thenReturn(new BigDecimal("10.000"));

        when(productionQueries.getTotalMilkProductionKg(
                jour2.atStartOfDay().atOffset(ZoneOffset.UTC),
                jour2.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        )).thenReturn(new BigDecimal("20.000"));

        when(conversionPolicy.convertKgToLitres(
                new BigDecimal("10.000")
        )).thenReturn(new BigDecimal("10.0000"));

        when(conversionPolicy.convertKgToLitres(
                new BigDecimal("20.000")
        )).thenReturn(new BigDecimal("20.0000"));

        PrixVenteLait prixJour1 =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("600.00"),
                        jour1,
                        null
                );

        PrixVenteLait prixJour2 =
                new PrixVenteLait(
                        tenantId,
                        new BigDecimal("650.00"),
                        jour2,
                        null
                );

        when(prixVenteLaitRepository.findApplicablePrice(
                tenantId,
                jour1
        )).thenReturn(Optional.of(prixJour1));

        when(prixVenteLaitRepository.findApplicablePrice(
                tenantId,
                jour2
        )).thenReturn(Optional.of(prixJour2));

        when(nutritionQueries.calculateTotalFeedCost(
                jour1,
                jour2.plusDays(1)
        )).thenReturn(
                Optional.of(
                        new ExploitationFeedCostReference(
                                jour1,
                                jour2.plusDays(1),
                                new BigDecimal("5000.0000")
                        )
                )
        );

        when(chargeExploitationService.sumAutresCharges(
                jour1,
                jour2.plusDays(1)
        )).thenReturn(
                new BigDecimal("2000.0000")
        );

        when(calculRepository.save(
                any(CalculRentabilite.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        RentabiliteService service =
                new RentabiliteService(
                        productionQueries,
                        nutritionQueries,
                        prixVenteLaitRepository,
                        chargeExploitationService,
                        calculRepository,
                        conversionPolicy,
                        tenantContext
                );

        CalculRentabilite result =
                service.calculate(
                        new CalculateRentabilite(
                                jour1,
                                fin
                        )
                );

        /*
         * Fixture de test :
         * la conversion simulée est volontairement 1 kg -> 1 litre.
         * Ce n'est PAS une valeur métier V1.
         */
        assertEquals(
                new BigDecimal("30.0000"),
                result.getVolumeLait()
        );

        assertEquals(
                new BigDecimal("19000.0000"),
                result.getChiffreAffaires()
        );

        assertEquals(
                new BigDecimal("5000.0000"),
                result.getCoutAlimentation()
        );

        assertEquals(
                new BigDecimal("2000.0000"),
                result.getAutresCharges()
        );

        assertEquals(
                new BigDecimal("7000.0000"),
                result.getCoutTotal()
        );

        assertEquals(
                new BigDecimal("233.3333"),
                result.getCoutRevientParLitre()
        );

        assertEquals(
                new BigDecimal("12000.0000"),
                result.getMarge()
        );

        verify(calculRepository)
                .save(any(CalculRentabilite.class));
    }
}