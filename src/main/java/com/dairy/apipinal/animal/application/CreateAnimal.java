package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.api.AnimalCreated;
import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CreateAnimal {

    private final AnimalRepository animalRepository;
    private final RaceRepository raceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateAnimal(
            AnimalRepository animalRepository,
            RaceRepository raceRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.animalRepository = animalRepository;
        this.raceRepository = raceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Animal execute(Command command) {

        raceRepository.findByIdAndActifTrue(command.raceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La race demandée n'existe pas ou est inactive."
                ));

        if (animalRepository.existsByExploitationIdAndIdentifiant(
                command.exploitationId(),
                command.identifiant()
        )) {
            throw new IllegalStateException(
                    "L'identifiant '" + command.identifiant()
                            + "' existe déjà dans cette exploitation."
            );
        }

        Animal animal = new Animal(
                command.tenantId(),
                command.exploitationId(),
                command.raceId(),
                command.identifiant(),
                command.nom(),
                command.photoUrl(),
                command.dateNaissance(),
                command.actorId()
        );

        Animal saved = animalRepository.save(animal);

        eventPublisher.publishEvent(
                new AnimalCreated(
                        saved.getId(),
                        saved.getTenantId(),
                        saved.getExploitationId(),
                        saved.getRaceId()
                )
        );

        return saved;
    }

    public record Command(
            UUID tenantId,
            UUID exploitationId,
            UUID raceId,
            String identifiant,
            String nom,
            String photoUrl,
            LocalDate dateNaissance,
            UUID actorId
    ) {
    }
}