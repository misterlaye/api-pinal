package com.dairy.apipinal.production;

import com.dairy.apipinal.production.application.ProductionQueriesImpl;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.production.infrastructure.persistence.TraiteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionQueriesTest {

    @Mock
    private LactationRepository lactationRepository;

    @Mock
    private TraiteRepository traiteRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private Lactation lactation1;

    @Mock
    private Lactation lactation2;

    @Test
    void shouldAggregateMilkProductionForAnimalAcrossLactations() {

        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID lactationId1 = UUID.randomUUID();
        UUID lactationId2 = UUID.randomUUID();

        OffsetDateTime debut =
                OffsetDateTime.parse("2026-09-01T00:00:00Z");

        OffsetDateTime finExclusive =
                OffsetDateTime.parse("2026-10-01T00:00:00Z");

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(lactation1.getId())
                .thenReturn(lactationId1);

        when(lactation2.getId())
                .thenReturn(lactationId2);

        when(lactationRepository.findAllOverlappingPeriod(
                animalId,
                tenantId,
                debut.toLocalDate(),
                finExclusive.minusNanos(1).toLocalDate()
        )).thenReturn(List.of(lactation1, lactation2));

        when(traiteRepository.sumQuantiteKgByLactationAndPeriod(
                lactationId1,
                tenantId,
                debut,
                finExclusive
        )).thenReturn(new BigDecimal("25.500"));

        when(traiteRepository.sumQuantiteKgByLactationAndPeriod(
                lactationId2,
                tenantId,
                debut,
                finExclusive
        )).thenReturn(new BigDecimal("18.500"));

        ProductionQueriesImpl queries =
                new ProductionQueriesImpl(
                        lactationRepository,
                        traiteRepository,
                        tenantContext
                );

        BigDecimal result =
                queries.getMilkProductionKg(
                        animalId,
                        debut,
                        finExclusive
                );

        assertEquals(
                new BigDecimal("44.000"),
                result
        );
    }

    @Test
    void shouldReturnTotalMilkProductionForTenant() {

        UUID tenantId = UUID.randomUUID();

        OffsetDateTime debut =
                OffsetDateTime.parse("2026-09-01T00:00:00Z");

        OffsetDateTime finExclusive =
                OffsetDateTime.parse("2026-10-01T00:00:00Z");

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(traiteRepository.sumQuantiteKgByTenantAndPeriod(
                tenantId,
                debut,
                finExclusive
        )).thenReturn(new BigDecimal("1250.750"));

        ProductionQueriesImpl queries =
                new ProductionQueriesImpl(
                        lactationRepository,
                        traiteRepository,
                        tenantContext
                );

        BigDecimal result =
                queries.getTotalMilkProductionKg(
                        debut,
                        finExclusive
                );

        assertEquals(
                new BigDecimal("1250.750"),
                result
        );
    }

    @Test
    void shouldRejectInvalidPeriod() {

        OffsetDateTime debut =
                OffsetDateTime.parse("2026-10-01T00:00:00Z");

        OffsetDateTime fin =
                OffsetDateTime.parse("2026-09-01T00:00:00Z");

        ProductionQueriesImpl queries =
                new ProductionQueriesImpl(
                        lactationRepository,
                        traiteRepository,
                        tenantContext
                );

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> queries.getTotalMilkProductionKg(debut, fin)
        );

        verifyNoInteractions(
                lactationRepository,
                traiteRepository,
                tenantContext
        );
    }
}