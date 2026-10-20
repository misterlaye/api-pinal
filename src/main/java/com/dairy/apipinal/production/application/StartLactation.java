package com.dairy.apipinal.production.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.production.api.event.LactationStarted;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class StartLactation {

    private final LactationRepository lactationRepository;
    private final AnimalQueries animalQueries;
    private final ApplicationEventPublisher eventPublisher;

    public StartLactation(
            LactationRepository lactationRepository,
            AnimalQueries animalQueries,
            ApplicationEventPublisher eventPublisher
    ) {
        this.lactationRepository = lactationRepository;
        this.animalQueries = animalQueries;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Lactation execute(Command command) {

        AnimalReference animal =
                animalQueries.getReference(command.animalId(), command.tenantId(), command.exploitationId());

        if (!"ACTIF".equals(animal.statut())) {
            throw new IllegalStateException(
                    "Un animal non actif ne peut pas démarrer une lactation."
            );
        }

        if (!animal.exploitationId().equals(command.exploitationId())) {
            throw new IllegalArgumentException(
                    "L'animal n'appartient pas à cette exploitation."
            );
        }

        if ("MALE".equals(animal.sexe())) {
            throw new IllegalStateException("Une lactation ne peut concerner qu'une femelle.");
        }

        if (command.dateDebut() == null || command.dateDebut().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de début de lactation est obligatoire et ne peut pas être dans le futur.");
        }

        lactationRepository.findByAnimalIdAndTenantIdAndExploitationIdAndStatut(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                com.dairy.apipinal.production.domain.StatutLactation.EN_COURS
        ).ifPresent(activeLactation -> {
            // Auto-close the previous lactation on the day before the new calving
            // Or on the same day if the new calving is today
            LocalDate dateFin = command.dateDebut().minusDays(1);
            if (dateFin.isBefore(activeLactation.getDateDebut())) {
                dateFin = activeLactation.getDateDebut();
            }
            activeLactation.terminer(dateFin, command.actorId());
            lactationRepository.save(activeLactation);
        });

        Lactation lactation = new Lactation(
                command.tenantId(),
                command.exploitationId(),
                command.animalId(),
                command.cycleId(), // Pass cycleId
                command.dateDebut(),
                command.actorId()
        );

        Lactation saved = lactationRepository.save(lactation);

        eventPublisher.publishEvent(
                new LactationStarted(
                        saved.getId(),
                        saved.getAnimalId(),
                        saved.getTenantId(),
                        saved.getExploitationId(),
                        saved.getDateDebut()
                )
        );

        return saved;
    }

    public record Command(
            UUID tenantId,
            UUID exploitationId,
            UUID animalId,
            UUID cycleId,
            LocalDate dateDebut,
            UUID actorId
    ) {
    }
}