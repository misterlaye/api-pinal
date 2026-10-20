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
                .findByIdAndTenantIdAndExploitationId(command.animalId(), command.tenantId(), command.exploitationId())
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

        if (command.mereId() != null) {
            if (command.mereId().equals(animal.getId())) {
                throw new IllegalArgumentException("Un animal ne peut pas être sa propre mère.");
            }
            Animal mere = animalRepository
                    .findByIdAndTenantIdAndExploitationId(command.mereId(), command.tenantId(), command.exploitationId())
                    .orElseThrow(() -> new IllegalArgumentException("Mère introuvable dans cette exploitation."));
            if (mere.getSexe() == com.dairy.apipinal.animal.domain.SexeAnimal.MALE) {
                throw new IllegalArgumentException("La mère déclarée est un mâle.");
            }
        }

        animal.update(
                command.raceId(),
                command.identifiant(),
                command.nom(),
                command.sexe(),
                command.photoUrl(),
                command.dateNaissance(),
                command.mereId(),
                command.pereIdentifiant(),
                command.actorId()
        );

        return animal;
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            UUID raceId,
            String identifiant,
            String nom,
            com.dairy.apipinal.animal.domain.SexeAnimal sexe,
            String photoUrl,
            LocalDate dateNaissance,
            UUID mereId,
            String pereIdentifiant,
            UUID actorId
    ) {
    }
}
