package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.api.AnimalSortiEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ChangeAnimalStatus {

    private final AnimalRepository animalRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ChangeAnimalStatus(AnimalRepository animalRepository, ApplicationEventPublisher eventPublisher) {
        this.animalRepository = animalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Animal execute(Command command) {

        Animal animal = animalRepository
                .findByIdAndTenantId(command.animalId(), command.tenantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));

        animal.changeStatus(command.status(), command.actorId());

        if (command.status() != StatutAnimal.ACTIF) {
            eventPublisher.publishEvent(new AnimalSortiEvent(
                    animal.getId(),
                    animal.getTenantId(),
                    animal.getExploitationId(),
                    command.status(),
                    LocalDate.now(),
                    command.actorId()
            ));
        }

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
