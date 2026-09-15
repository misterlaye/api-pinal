package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.infrastructure.persistence.RaceRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class UpdateAnimal {

    private final AnimalRepository animalRepository;
    private final RaceRepository raceRepository;

    public UpdateAnimal(
            AnimalRepository animalRepository,
            RaceRepository raceRepository
    ) {
        this.animalRepository = animalRepository;
        this.raceRepository = raceRepository;
    }

    @Transactional
    public Animal execute(Command command) {

        Animal animal = animalRepository
                .findByIdAndTenantId(command.animalId(), command.tenantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));

        raceRepository.findByIdAndActifTrue(command.raceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "La race demandée n'existe pas ou est inactive."
                ));

        boolean duplicate =
                animalRepository.existsByExploitationIdAndIdentifiantAndIdNot(
                        animal.getExploitationId(),
                        command.identifiant(),
                        command.animalId()
                );

        if (duplicate) {
            throw new IllegalStateException(
                    "Cet identifiant existe déjà dans l'exploitation."
            );
        }

        animal.update(
                command.raceId(),
                command.identifiant(),
                command.nom(),
                command.photoUrl(),
                command.dateNaissance(),
                command.actorId()
        );

        return animal;
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID raceId,
            String identifiant,
            String nom,
            String photoUrl,
            LocalDate dateNaissance,
            UUID actorId
    ) {
    }
}
