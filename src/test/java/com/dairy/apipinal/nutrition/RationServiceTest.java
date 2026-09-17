package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.nutrition.application.*;
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
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Test
    void shouldTerminateActiveRation() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        LocalDate dateDebut = LocalDate.of(2026, 1, 1);
        LocalDate dateFin = LocalDate.of(2026, 1, 31);

        when(tenantContext.currentTenantId()).thenReturn(tenantId);

        Ration ration = new Ration(
                tenantId,
                animalId,
                dateDebut,
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5.0000")
        );

        ration.activer();

        // Le repository peut retourner la ration sans dépendre de son UUID généré.
        when(rationRepository.findByIdAndTenantId(rationId, tenantId))
                .thenReturn(Optional.of(ration));

        when(rationRepository.save(ration))
                .thenReturn(ration);

        Ration result = rationService.terminate(
                new TerminateRation(
                        animalId,
                        rationId,
                        dateFin
                )
        );

        assertThat(result.getStatut())
                .isEqualTo(StatutRation.TERMINEE);

        assertThat(result.getDateFin())
                .isEqualTo(dateFin);

        verify(rationRepository).save(ration);
    }

    @Test
    void shouldRefuseTerminationWhenRationIsNotActive() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId()).thenReturn(tenantId);

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 1, 1),
                OrigineRation.ACTUELLE
        );

        when(rationRepository.findByIdAndTenantId(rationId, tenantId))
                .thenReturn(Optional.of(ration));

        assertThatThrownBy(() ->
                rationService.terminate(
                        new TerminateRation(
                                animalId,
                                rationId,
                                LocalDate.of(2026, 1, 31)
                        )
                )
        )
                .isInstanceOf(IllegalStateException.class);

        verify(rationRepository, never()).save(any());
    }

    @Test
    void shouldRefuseTerminationWhenEndDateIsBeforeStartDate() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId()).thenReturn(tenantId);

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 2, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5.0000")
        );

        ration.activer();

        when(rationRepository.findByIdAndTenantId(rationId, tenantId))
                .thenReturn(Optional.of(ration));

        assertThatThrownBy(() ->
                rationService.terminate(
                        new TerminateRation(
                                animalId,
                                rationId,
                                LocalDate.of(2026, 1, 31)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(rationRepository, never()).save(any());
    }

    @Test
    void shouldRefuseTerminationWhenAnimalIdDoesNotMatch() {
        UUID tenantId = UUID.randomUUID();
        UUID rationAnimalId = UUID.randomUUID();
        UUID requestedAnimalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId()).thenReturn(tenantId);

        Ration ration = new Ration(
                tenantId,
                rationAnimalId,
                LocalDate.of(2026, 1, 1),
                OrigineRation.ACTUELLE
        );

        when(rationRepository.findByIdAndTenantId(rationId, tenantId))
                .thenReturn(Optional.of(ration));

        assertThatThrownBy(() ->
                rationService.terminate(
                        new TerminateRation(
                                requestedAnimalId,
                                rationId,
                                LocalDate.of(2026, 1, 31)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(rationRepository, never()).save(any());
    }

    @Test
    void shouldRefuseTerminationWhenRationDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId()).thenReturn(tenantId);

        when(rationRepository.findByIdAndTenantId(rationId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                rationService.terminate(
                        new TerminateRation(
                                animalId,
                                rationId,
                                LocalDate.of(2026, 1, 31)
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(rationRepository, never()).save(any());
    }

    @Test
    void shouldGetRationForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(Optional.of(ration));

        Ration result = rationService.getRation(
                new GetRation(rationId)
        );

        assertThat(result)
                .isSameAs(ration);
    }

    @Test
    void shouldNotReturnRationFromAnotherTenant() {
        UUID currentTenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(currentTenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                currentTenantId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                rationService.getRation(
                        new GetRation(rationId)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ration introuvable.");
    }

    @Test
    void shouldRefuseWhenRationDoesNotExist() {
        UUID tenantId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                rationService.getRation(
                        new GetRation(rationId)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ration introuvable.");
    }

    @Test
    void shouldGetRationsByAnimalForCurrentTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "dateDebut")
        );

        Ration ration = new Ration(
                tenantId,
                animalId,
                LocalDate.of(2026, 9, 16),
                OrigineRation.ACTUELLE
        );

        Page<Ration> expected = new PageImpl<>(
                List.of(ration),
                pageable,
                1
        );

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(animalQueries.getReference(animalId))
                .thenReturn(mock(
                        com.dairy.apipinal.animal.api.AnimalReference.class
                ));

        when(rationRepository.findByTenantIdAndAnimalId(
                tenantId,
                animalId,
                pageable
        )).thenReturn(expected);

        Page<Ration> result = rationService.getRationsByAnimal(
                new GetRationsByAnimal(
                        animalId,
                        pageable
                )
        );

        assertThat(result)
                .isSameAs(expected);
    }

    @Test
    void shouldReturnEmptyPageWhenAnimalHasNoRations() {
        UUID tenantId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(0, 20);

        Page<Ration> expected = Page.empty(pageable);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(animalQueries.getReference(animalId))
                .thenReturn(mock(
                        com.dairy.apipinal.animal.api.AnimalReference.class
                ));

        when(rationRepository.findByTenantIdAndAnimalId(
                tenantId,
                animalId,
                pageable
        )).thenReturn(expected);

        Page<Ration> result = rationService.getRationsByAnimal(
                new GetRationsByAnimal(
                        animalId,
                        pageable
                )
        );

        assertThat(result.getContent())
                .isEmpty();

        assertThat(result.getTotalElements())
                .isZero();
    }
}
