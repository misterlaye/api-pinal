package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChangeAnimalStatus {

    private final AnimalRepository animalRepository;

    public ChangeAnimalStatus(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Transactional
    public Animal execute(Command command) {

        Animal animal = animalRepository
                .findByIdAndTenantId(command.animalId(), command.tenantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));

        animal.changeStatus(command.status(), command.actorId());

        return animal;
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            StatutAnimal status,
            UUID actorId
    ) {
    }
}
