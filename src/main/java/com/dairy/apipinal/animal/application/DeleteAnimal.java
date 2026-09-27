package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeleteAnimal {

    private final AnimalRepository animalRepository;

    public DeleteAnimal(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Transactional
    public void execute(Command command) {
        animalRepository.findByIdAndTenantId(command.animalId(), command.tenantId())
                .ifPresentOrElse(
                        animal -> {
                            try {
                                animalRepository.delete(animal);
                                animalRepository.flush();
                            } catch (DataIntegrityViolationException ex) {
                                throw new IllegalStateException(
                                        "Impossible de supprimer cet animal car il possède des données associées (traites, reproductions, etc.). " +
                                                "Veuillez plutôt déclarer une sortie (vente/décès)."
                                );
                            }
                        },
                        () -> {
                            throw new IllegalArgumentException("Animal introuvable.");
                        }
                );
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID actorId
    ) {
    }
}
