package com.dairy.apipinal.production.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Traite individuelle saisie à partir de l'animal : elle est rattachée à la lactation en cours.
 * <p>
 * Aucune lactation n'est créée implicitement : une lactation démarre par un vêlage ou par une déclaration
 * explicite ({@link StartLactation}), qui appliquent les règles communes (animal actif, femelle, exploitation).
 */
@Service
public class RecordAnimalMilking {

    private final LactationRepository lactationRepository;
    private final RecordMilking recordMilking;
    private final AnimalQueries animalQueries;

    public RecordAnimalMilking(
            LactationRepository lactationRepository,
            RecordMilking recordMilking,
            AnimalQueries animalQueries
    ) {
        this.lactationRepository = lactationRepository;
        this.recordMilking = recordMilking;
        this.animalQueries = animalQueries;
    }

    @Transactional
    public Traite execute(Command command) {
        // Vérifie que l'animal appartient à l'exploitation courante (sinon : « Animal introuvable »).
        animalQueries.getReference(command.animalId(), command.tenantId(), command.exploitationId());

        Lactation activeLactation = lactationRepository
                .findByAnimalIdAndTenantIdAndExploitationIdAndStatut(
                        command.animalId(),
                        command.tenantId(),
                        command.exploitationId(),
                        StatutLactation.EN_COURS
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Aucune lactation en cours pour cet animal : déclarez le vêlage ou démarrez la lactation."
                ));

        return recordMilking.execute(
                new RecordMilking.Command(
                        command.tenantId(),
                        command.exploitationId(),
                        activeLactation.getId(),
                        command.auteurId(),
                        command.dateHeure(),
                        command.type(),
                        command.quantiteLitres()
                )
        );
    }

    public record Command(
            UUID tenantId,
            UUID exploitationId,
            UUID animalId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            BigDecimal quantiteLitres
    ) {
    }
}
