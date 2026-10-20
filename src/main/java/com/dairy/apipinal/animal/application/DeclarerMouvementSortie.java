package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.MouvementSortie;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.animal.infrastructure.persistence.MouvementSortieRepository;
import com.dairy.apipinal.animal.api.AnimalSortiEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DeclarerMouvementSortie {

    private final AnimalRepository animalRepository;
    private final MouvementSortieRepository mouvementSortieRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeclarerMouvementSortie(
            AnimalRepository animalRepository,
            MouvementSortieRepository mouvementSortieRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.animalRepository = animalRepository;
        this.mouvementSortieRepository = mouvementSortieRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Seule voie de sortie du troupeau : statut, mouvement de sortie et événement {@link AnimalSortiEvent}
     * (clôture de la lactation, des rations et des cycles en cours dans la même transaction).
     * Motifs : VENTE (statut VENDU) ou DECES (statut DECEDE).
     */
    @Transactional
    public MouvementSortie execute(Command command) {
        Animal animal = animalRepository
                .findByIdAndTenantIdAndExploitationId(command.animalId(), command.tenantId(), command.exploitationId())
                .orElseThrow(() -> new IllegalArgumentException("Animal introuvable."));

        if (animal.getStatut() != StatutAnimal.ACTIF) {
            throw new IllegalStateException("L'animal n'est pas actif (déjà vendu ou décédé).");
        }

        String motif = command.motif() == null ? "" : command.motif().trim().toUpperCase();
        StatutAnimal nouveauStatut = switch (motif) {
            case "VENTE" -> StatutAnimal.VENDU;
            case "DECES" -> StatutAnimal.DECEDE;
            default -> throw new IllegalArgumentException("Motif de sortie inconnu : utilisez VENTE ou DECES.");
        };

        LocalDate dateSortie = command.dateSortie() != null ? command.dateSortie() : LocalDate.now();
        if (dateSortie.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de sortie ne peut pas être dans le futur.");
        }
        if (animal.getDateNaissance() != null && dateSortie.isBefore(animal.getDateNaissance())) {
            throw new IllegalArgumentException("La date de sortie ne peut pas précéder la date de naissance.");
        }

        BigDecimal prixVente = nouveauStatut == StatutAnimal.VENDU ? command.prixVente() : null;
        if (prixVente != null && prixVente.signum() < 0) {
            throw new IllegalArgumentException("Le prix de vente ne peut pas être négatif.");
        }

        animal.changeStatus(nouveauStatut, command.actorId());
        animalRepository.save(animal);

        MouvementSortie saved = mouvementSortieRepository.save(new MouvementSortie(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                dateSortie,
                motif,
                prixVente,
                command.actorId()
        ));

        eventPublisher.publishEvent(new AnimalSortiEvent(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                nouveauStatut,
                dateSortie,
                command.actorId()
        ));

        return saved;
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
