package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.nutrition.application.CreateRation;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.RationAlreadyActiveException;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    }

    @Test
    void shouldCreateDraftRation() {

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
                .thenAnswer(
                        invocation -> invocation.getArgument(0)
                );

        CreateRation command = new CreateRation(
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        Ration savedRation = rationService.create(command);

        assertThat(savedRation.getTenantId())
                .isEqualTo(tenantId);

        assertThat(savedRation.getAnimalId())
                .isEqualTo(animalId);

        assertThat(savedRation.getStatut())
                .isEqualTo(StatutRation.BROUILLON);
    }

    @Test
    void shouldActivateRationWithoutConflict() {

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                alimentId,
                BigDecimal.ONE
        );

        when(
                rationRepository.findByIdAndTenantId(
                        rationId,
                        tenantId
                )
        ).thenReturn(Optional.of(ration));

        when(
                rationRepository.existsOverlappingActiveRationWithoutEndDate(
                        tenantId,
                        animalId,
                        ration.getDateDebut(),
                        StatutRation.ACTIVE
                )
        ).thenReturn(false);

        when(rationRepository.save(ration))
                .thenReturn(ration);

        Ration result = rationService.activate(
                animalId,
                rationId
        );

        assertThat(result.getStatut())
                .isEqualTo(StatutRation.ACTIVE);

        verify(rationRepository)
                .save(ration);
    }

    @Test
    void shouldRefuseActivationWhenActiveRationOverlapsPeriod() {

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        UUID rationId = UUID.randomUUID();
        UUID alimentId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                alimentId,
                BigDecimal.ONE
        );

        when(
                rationRepository.findByIdAndTenantId(
                        rationId,
                        tenantId
                )
        ).thenReturn(Optional.of(ration));

        when(
                rationRepository.existsOverlappingActiveRationWithoutEndDate(
                        tenantId,
                        animalId,
                        ration.getDateDebut(),
                        StatutRation.ACTIVE
                )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                rationService.activate(animalId, rationId))
                .isInstanceOf(RationAlreadyActiveException.class)
                .hasMessage("Une autre ration active est déjà applicable à cet animal sur cette période.");
    }

    @Test
    void shouldRefuseActivationWhenAnimalIdDoesNotMatch() {

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        UUID rationId = UUID.randomUUID();
        UUID rationAnimalId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                rationAnimalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        when(
                rationRepository.findByIdAndTenantId(
                        rationId,
                        tenantId
                )
        ).thenReturn(Optional.of(ration));

        assertThatThrownBy(() ->
                rationService.activate(
                        animalId,
                        rationId
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "La ration n'appartient pas à l'animal indiqué."
                );
    }
}
