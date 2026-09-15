package com.dairy.apipinal.health.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.health.api.event.HealthEventRecorded;
import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.health.infrastructure.persistence.EvenementSanitaireRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RecordHealthEvent {

    private final EvenementSanitaireRepository repository;
    private final AnimalQueries animalQueries;
    private final ApplicationEventPublisher eventPublisher;

    public RecordHealthEvent(
            EvenementSanitaireRepository repository,
            AnimalQueries animalQueries,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.animalQueries = animalQueries;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EvenementSanitaire execute(Command command) {

        AnimalReference animal =
                animalQueries.getReference(command.animalId());

        if (!"ACTIF".equals(animal.statut())) {
            throw new IllegalStateException(
                    "Un événement sanitaire ne peut pas être enregistré "
                            + "pour un animal qui n'est plus actif."
            );
        }

        EvenementSanitaire event = new EvenementSanitaire(
                command.tenantId(),
                command.animalId(),
                command.dateHeure(),
                command.description(),
                command.actorId()
        );

        EvenementSanitaire saved = repository.save(event);

        eventPublisher.publishEvent(
                new HealthEventRecorded(
                        saved.getId(),
                        saved.getTenantId(),
                        saved.getAnimalId(),
                        saved.getDateHeure()
                )
        );

        return saved;
    }

    public record Command(
            UUID tenantId,
            UUID animalId,
            OffsetDateTime dateHeure,
            String description,
            UUID actorId
    ) {
    }
}