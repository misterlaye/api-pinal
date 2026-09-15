package com.dairy.apipinal.animal;

import com.dairy.apipinal.animal.application.CreateAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ApplicationModuleTest
class AnimalIntegrationTest {

    private final CreateAnimal createAnimal;
    private final RaceRepository raceRepository;

    @MockitoBean
    TenantContext tenantContext;

    AnimalIntegrationTest(
            CreateAnimal createAnimal,
            RaceRepository raceRepository
    ) {
        this.createAnimal = createAnimal;
        this.raceRepository = raceRepository;
    }

    @Test
    void shouldCreateAnimal() {

        UUID tenantId =
                UUID.fromString("00000000-0000-0000-0000-000000000001");

        UUID userId =
                UUID.fromString("00000000-0000-0000-0000-000000000001");

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(tenantContext.currentUserId())
                .thenReturn(userId);

        UUID raceId = raceRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow()
                .getId();

        UUID exploitationId = UUID.randomUUID();

        var animal = createAnimal.execute(
                new CreateAnimal.Command(
                        tenantId,
                        exploitationId,
                        raceId,
                        "DI001",
                        "Diouma",
                        null,
                        null,
                        userId
                )
        );

        org.junit.jupiter.api.Assertions.assertNotNull(
                animal.getId()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "DI001",
                animal.getIdentifiant()
        );
    }
}