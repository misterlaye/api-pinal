package com.dairy.apipinal.health;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.health.application.RecordHealthEvent;
import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ApplicationModuleTest
class HealthIntegrationTest {

    @MockitoBean
    TenantContext tenantContext;

    @MockitoBean
    AnimalQueries animalQueries;

    private final RecordHealthEvent recordHealthEvent;

    HealthIntegrationTest(
            RecordHealthEvent recordHealthEvent
    ) {
        this.recordHealthEvent = recordHealthEvent;
    }

    @Test
    void shouldRecordHealthEvent() {

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

        EvenementSanitaire event =
                recordHealthEvent.execute(
                        new RecordHealthEvent.Command(
                                tenantId,
                                animalId,
                                OffsetDateTime.parse(
                                        "2026-02-01T10:00:00Z"
                                ),
                                "Suspicion de problème sanitaire",
                                userId
                        )
                );

        assertNotNull(event.getId());
        assertNotNull(event.getDescription());
    }
}