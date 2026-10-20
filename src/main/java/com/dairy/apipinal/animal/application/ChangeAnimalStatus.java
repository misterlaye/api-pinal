package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Changement de statut d'un animal.
 * <p>
 * Ce n'est pas un chemin parallèle : une sortie (VENDU, DECEDE) est déléguée à {@link DeclarerMouvementSortie},
 * qui enregistre le mouvement et applique ses effets dans tous les modules. Une sortie est définitive.
 */
@Service
public class ChangeAnimalStatus {

    private final AnimalRepository animalRepository;
    private final DeclarerMouvementSortie declarerMouvementSortie;

    public ChangeAnimalStatus(AnimalRepository animalRepository, DeclarerMouvementSortie declarerMouvementSortie) {
        this.animalRepository = animalRepository;
        this.declarerMouvementSortie = declarerMouvementSortie;
    }

    @Transactional
    public Animal execute(Command command) {

        Animal animal = animalRepository
                .findByIdAndTenantIdAndExploitationId(command.animalId(), command.tenantId(), command.exploitationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));

        if (command.status() == null) {
            throw new IllegalArgumentException("Le statut de l'animal est obligatoire.");
        }
        if (animal.getStatut() == command.status()) {
            return animal;
        }
        if (command.status() == StatutAnimal.ACTIF) {
            throw new IllegalStateException(
                    "Un animal sorti du troupeau (" + animal.getStatut() + ") ne peut pas être réactivé."
            );
        }

        declarerMouvementSortie.execute(new DeclarerMouvementSortie.Command(
                animal.getId(),
                command.tenantId(),
                command.exploitationId(),
                LocalDate.now(),
                command.status() == StatutAnimal.VENDU ? "VENTE" : "DECES",
                null,
                command.actorId()
        ));

        return animal;
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            StatutAnimal status,
            UUID actorId
    ) {
    }
}
