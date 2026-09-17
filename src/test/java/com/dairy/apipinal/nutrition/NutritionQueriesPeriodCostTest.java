package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.api.RationCostReference;
import com.dairy.apipinal.nutrition.api.RationReference;
import com.dairy.apipinal.nutrition.application.NutritionQueriesImpl;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NutritionQueriesPeriodCostTest {

    @Mock
    private RationRepository rationRepository;

    @Mock
    private RationService rationService;

    @Mock
    private TenantContext tenantContext;

    @Test
    void shouldCalculateFeedCostForEntirePeriod() {

        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        LocalDate jour1 = LocalDate.of(2026, 9, 1);
        LocalDate jour2 = LocalDate.of(2026, 9, 2);
        LocalDate finExclusive = LocalDate.of(2026, 9, 3);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        var rationReference = new RationReference(
                rationId,
                tenantId,
                animalId,
                jour1,
                null,
                StatutRation.ACTIVE,
                OrigineRation.ACTUELLE
        );

        var rationCostDay1 = new RationCostReference(
                rationId,
                animalId,
                jour1,
                new BigDecimal("3500.00")
        );

        var rationCostDay2 = new RationCostReference(
                rationId,
                animalId,
                jour2,
                new BigDecimal("3700.00")
        );

        // IMPORTANT : créer les mocks avant le when(...).thenReturn(...)
        var rationDay1 = mockRation(
                rationId,
                tenantId,
                animalId
        );

        var rationDay2 = mockRation(
                rationId,
                tenantId,
                animalId
        );

        when(rationRepository.findActiveRationAtDate(
                tenantId,
                animalId,
                jour1,
                StatutRation.ACTIVE
        )).thenReturn(Optional.of(rationDay1));

        when(rationRepository.findActiveRationAtDate(
                tenantId,
                animalId,
                jour2,
                StatutRation.ACTIVE
        )).thenReturn(Optional.of(rationDay2));

        when(rationService.calculateCost(any()))
                .thenReturn(
                        mockRationCostResult(
                                rationId,
                                jour1,
                                new BigDecimal("3500.00")
                        ),
                        mockRationCostResult(
                                rationId,
                                jour2,
                                new BigDecimal("3700.00")
                        )
                );

        NutritionQueriesImpl queries =
                new NutritionQueriesImpl(
                        rationRepository,
                        rationService,
                        tenantContext
                );

        var result = queries.calculateFeedCost(
                animalId,
                jour1,
                finExclusive
        );

        assertTrue(result.isPresent());

        assertEquals(
                new BigDecimal("7200.00"),
                result.get().coutAlimentation()
        );
    }

    @Test
    void shouldReturnEmptyWhenOneDayHasNoActiveRation() {

        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        LocalDate jour1 = LocalDate.of(2026, 9, 1);
        LocalDate jour2 = LocalDate.of(2026, 9, 2);
        LocalDate finExclusive = LocalDate.of(2026, 9, 3);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findActiveRationAtDate(
                tenantId,
                animalId,
                jour1,
                com.dairy.apipinal.nutrition.domain.StatutRation.ACTIVE
        )).thenReturn(Optional.empty());

        NutritionQueriesImpl queries =
                new NutritionQueriesImpl(
                        rationRepository,
                        rationService,
                        tenantContext
                );

        var result = queries.calculateFeedCost(
                animalId,
                jour1,
                finExclusive
        );

        assertTrue(result.isEmpty());

        verifyNoInteractions(rationService);
    }

    @Test
    void shouldRejectInvalidPeriod() {

        UUID animalId = UUID.randomUUID();

        LocalDate debut = LocalDate.of(2026, 9, 10);
        LocalDate fin = LocalDate.of(2026, 9, 1);

        NutritionQueriesImpl queries =
                new NutritionQueriesImpl(
                        rationRepository,
                        rationService,
                        tenantContext
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> queries.calculateFeedCost(
                        animalId,
                        debut,
                        fin
                )
        );

        verifyNoInteractions(
                tenantContext,
                rationRepository,
                rationService
        );
    }

    private com.dairy.apipinal.nutrition.domain.Ration mockRation(
            UUID rationId,
            UUID tenantId,
            UUID animalId
    ) {
        var ration =
                mock(com.dairy.apipinal.nutrition.domain.Ration.class);

        when(ration.getId()).thenReturn(rationId);
        when(ration.getTenantId()).thenReturn(tenantId);
        when(ration.getAnimalId()).thenReturn(animalId);

        return ration;
    }

    private com.dairy.apipinal.nutrition.application.RationCostResult
    mockRationCostResult(
            UUID rationId,
            LocalDate date,
            BigDecimal cost
    ) {
        return new com.dairy.apipinal.nutrition.application.RationCostResult(
                rationId,
                date,
                java.util.List.of(),
                cost
        );
    }
}