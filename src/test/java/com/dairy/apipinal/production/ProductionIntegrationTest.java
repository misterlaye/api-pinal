package com.dairy.apipinal.production;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.production.application.RecordMilking;
import com.dairy.apipinal.production.application.StartLactation;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ApplicationModuleTest
class ProductionIntegrationTest {

    @MockitoBean
    TenantContext tenantContext;

    @MockitoBean
    AnimalQueries animalQueries;

    private final StartLactation startLactation;
    private final RecordMilking recordMilking;
    private final LactationRepository lactationRepository;

    ProductionIntegrationTest(
            StartLactation startLactation,
            RecordMilking recordMilking,
            LactationRepository lactationRepository
    ) {
        this.startLactation = startLactation;
        this.recordMilking = recordMilking;
        this.lactationRepository = lactationRepository;
    }

    @Test
    void shouldStartLactationAndRecordMilking() {

        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(tenantContext.currentUserId())
                .thenReturn(userId);

        when(animalQueries.getReference(animalId))
                .thenReturn(
                        new AnimalReference(
                                animalId,
                                tenantId,
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "DI001",
                                "Diouma",
                                "ACTIF"
                        )
                );

        var lactation = startLactation.execute(
                new StartLactation.Command(
                        tenantId,
                        animalId,
                        LocalDate.of(2026, 1, 1),
                        userId
                )
        );

        assertNotNull(lactation.getId());

        Traite traite = recordMilking.execute(
                new RecordMilking.Command(
                        tenantId,
                        lactation.getId(),
                        userId,
                        OffsetDateTime.parse("2026-01-02T06:00:00Z"),
                        TypeTraite.MATIN,
                        new BigDecimal("12.500")
                )
        );

        assertNotNull(traite.getId());
        assertEquals(
                new BigDecimal("12.500"),
                traite.getQuantiteKg()
        );
    }
}