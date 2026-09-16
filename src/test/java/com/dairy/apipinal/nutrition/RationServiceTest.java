package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.nutrition.application.CreateRation;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RationServiceTest {

    @Mock
    private RationRepository rationRepository;

    @Mock
    private AlimentRepository alimentRepository;

    @Mock
    private AnimalQueries animalQueries;

    @Mock
    private TenantContext tenantContext;

    private RationService rationService;

    private UUID tenantId;
    private UUID animalId;
    private UUID exploitationId;
    private UUID raceId;

    @BeforeEach
    void setUp() {
        rationService = new RationService(
                rationRepository,
                alimentRepository,
                animalQueries,
                tenantContext
        );

        tenantId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        exploitationId = UUID.randomUUID();
        raceId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(animalQueries.getReference(animalId))
                .thenReturn(
                        new AnimalReference(
                                animalId,
                                tenantId,
                                exploitationId,
                                raceId,
                                "A001",
                                "Diouma",
                                "ACTIF"
                        )
                );

        when(rationRepository.save(any(Ration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateDraftRation() {

        CreateRation command = new CreateRation(
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        Ration savedRation = rationService.create(command);

        assertThat(savedRation)
                .isNotNull();

        assertThat(savedRation.getTenantId())
                .isEqualTo(tenantId);

        assertThat(savedRation.getAnimalId())
                .isEqualTo(animalId);

        assertThat(savedRation.getDateDebut())
                .isEqualTo(LocalDate.of(2026, 9, 1));

        assertThat(savedRation.getOrigine())
                .isEqualTo(OrigineRation.ACTUELLE);

        assertThat(savedRation.getStatut())
                .isEqualTo(StatutRation.BROUILLON);

        verify(tenantContext)
                .currentTenantId();

        verify(animalQueries)
                .getReference(animalId);

        verify(rationRepository)
                .save(any(Ration.class));
    }
}