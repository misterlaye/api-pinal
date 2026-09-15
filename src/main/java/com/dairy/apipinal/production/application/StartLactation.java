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
                animalQueries.getReference(command.animalId());

        if (!"ACTIF".equals(animal.statut())) {
            throw new IllegalStateException(
                    "Un animal non actif ne peut pas démarrer une lactation."
            );
        }

        if (lactationRepository
                .existsByAnimalIdAndTenantIdAndStatut(
                        command.animalId(),
                        command.tenantId(),
                        com.dairy.apipinal.production.domain.StatutLactation.EN_COURS
                )) {

            throw new IllegalStateException(
                    "L'animal possède déjà une lactation en cours."
            );
        }

        Lactation lactation = new Lactation(
                command.tenantId(),
                command.animalId(),
                command.dateDebut(),
                command.actorId()
        );

        Lactation saved = lactationRepository.save(lactation);

        eventPublisher.publishEvent(
                new LactationStarted(
                        saved.getId(),
                        saved.getAnimalId(),
                        saved.getTenantId(),
                        saved.getDateDebut()
                )
        );

        return saved;
    }

    public record Command(
            UUID tenantId,
            UUID animalId,
            LocalDate dateDebut,
            UUID actorId
    ) {
    }
}