package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.application.PrixAlimentService;
import com.dairy.apipinal.nutrition.application.CreatePrixAliment;
import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.PrixAlimentRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrixAlimentServiceTest {

    @Mock
    private AlimentRepository alimentRepository;

    @Mock
    private PrixAlimentRepository prixAlimentRepository;

    private PrixAlimentService prixAlimentService;

    @BeforeEach
    void setUp() {
        prixAlimentService = new PrixAlimentService(
                alimentRepository,
                prixAlimentRepository
        );
    }

    @Test
    void shouldCreatePrixAliment() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        Aliment aliment = mock(Aliment.class);

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.of(aliment));

        when(prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                alimentId,
                command.dateDebut(),
                command.dateFin()
        )).thenReturn(false);

        PrixAliment prix = mock(PrixAliment.class);

        when(prixAlimentRepository.save(any(PrixAliment.class)))
                .thenReturn(prix);

        PrixAliment result = prixAlimentService.create(command);

        assertThat(result)
                .isSameAs(prix);

        verify(alimentRepository)
                .findById(alimentId);

        verify(prixAlimentRepository)
                .existsOverlappingPeriodWithEndDate(
                        alimentId,
                        command.dateDebut(),
                        command.dateFin()
                );

        verify(prixAlimentRepository)
                .save(any(PrixAliment.class));
    }

    @Test
    void shouldRefuseWhenAlimentDoesNotExist() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aliment introuvable.");

        verify(prixAlimentRepository, never())
                .save(any());
    }

    @Test
    void shouldRefuseInvalidDateRange() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 9, 30),
                LocalDate.of(2026, 9, 1)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.of(
                        mock(Aliment.class)
                ));

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "La date de fin ne peut pas être antérieure à la date de début."
                );

        verify(prixAlimentRepository, never())
                .save(any());

        verify(
                prixAlimentRepository,
                never()
        ).existsOverlappingPeriodWithEndDate(
                any(),
                any(),
                any()
        );

        verify(
                prixAlimentRepository,
                never()
        ).existsOverlappingOpenEndedPeriod(
                any(),
                any()
        );
    }

    @Test
    void shouldRefuseOverlappingPeriod() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 9, 15),
                LocalDate.of(2026, 10, 15)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.of(
                        mock(Aliment.class)
                ));

        when(prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                alimentId,
                command.dateDebut(),
                command.dateFin()
        )).thenReturn(true);

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "La période du prix chevauche une période existante."
                );

        verify(prixAlimentRepository, never())
                .save(any());
    }

    @Test
    void shouldRefuseOverlappingOpenEndedPeriod() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("150.00"),
                LocalDate.of(2026, 10, 15),
                null
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.of(
                        mock(Aliment.class)
                ));

        when(prixAlimentRepository.existsOverlappingOpenEndedPeriod(
                alimentId,
                command.dateDebut()
        )).thenReturn(true);

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "La période du prix chevauche une période existante."
                );

        verify(prixAlimentRepository, never())
                .save(any());
    }

    @Test
    void shouldAllowAdjacentPeriods() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("125.00"),
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(java.util.Optional.of(
                        mock(Aliment.class)
                ));

        when(prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                alimentId,
                command.dateDebut(),
                command.dateFin()
        )).thenReturn(false);

        PrixAliment prix = mock(PrixAliment.class);

        when(prixAlimentRepository.save(any(PrixAliment.class)))
                .thenReturn(prix);

        PrixAliment result =
                prixAlimentService.create(command);

        assertThat(result)
                .isSameAs(prix);

        verify(prixAlimentRepository)
                .save(any(PrixAliment.class));
    }

    @Test
    void shouldRefuseNonPositivePrice() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                BigDecimal.ZERO,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(Optional.of(mock(Aliment.class)));

        when(prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                alimentId,
                command.dateDebut(),
                command.dateFin()
        )).thenReturn(false);

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(prixAlimentRepository, never())
                .save(any(PrixAliment.class));
    }

    @Test
    void shouldRefuseNegativePrice() {

        UUID alimentId = UUID.randomUUID();

        CreatePrixAliment command = new CreatePrixAliment(
                alimentId,
                new BigDecimal("-10.00"),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30)
        );

        when(alimentRepository.findById(alimentId))
                .thenReturn(Optional.of(mock(Aliment.class)));

        when(prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                alimentId,
                command.dateDebut(),
                command.dateFin()
        )).thenReturn(false);

        assertThatThrownBy(() ->
                prixAlimentService.create(command)
        )
                .isInstanceOf(IllegalArgumentException.class);

        verify(prixAlimentRepository, never())
                .save(any(PrixAliment.class));
    }
}