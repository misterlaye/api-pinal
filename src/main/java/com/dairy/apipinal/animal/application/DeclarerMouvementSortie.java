package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.MouvementSortie;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.infrastructure.persistence.MouvementSortieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DeclarerMouvementSortie {

    private final AnimalRepository animalRepository;
    private final MouvementSortieRepository mouvementSortieRepository;

    public DeclarerMouvementSortie(AnimalRepository animalRepository, MouvementSortieRepository mouvementSortieRepository) {
        this.animalRepository = animalRepository;
        this.mouvementSortieRepository = mouvementSortieRepository;
    }

    @Transactional
    public MouvementSortie execute(Command command) {
        Animal animal = animalRepository.findById(command.animalId())
                .orElseThrow(() -> new IllegalArgumentException("Animal non trouvé"));

        if (animal.getStatut() != StatutAnimal.ACTIF) {
            throw new IllegalStateException("L'animal n'est pas actif (déjà vendu ou décédé)");
        }

        // Marquer l'animal comme VENDU ou DECEDE selon le motif
        StatutAnimal nouveauStatut = "VENTE".equalsIgnoreCase(command.motif())
                ? StatutAnimal.VENDU
                : StatutAnimal.DECEDE;

        animal.changeStatus(nouveauStatut, command.actorId());
        animalRepository.save(animal);

        MouvementSortie mouvement = new MouvementSortie(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                command.dateSortie(),
                command.motif(),
                command.prixVente(),
                command.actorId()
        );

        return mouvementSortieRepository.save(mouvement);
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            LocalDate dateSortie,
            String motif,
            BigDecimal prixVente,
            UUID actorId
    ) {
    }
}
