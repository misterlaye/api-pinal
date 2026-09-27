package com.dairy.apipinal.production.application;

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

@Service
public class RecordAnimalMilking {

    private final LactationRepository lactationRepository;
    private final RecordMilking recordMilking;

    public RecordAnimalMilking(
            LactationRepository lactationRepository,
            RecordMilking recordMilking
    ) {
        this.lactationRepository = lactationRepository;
        this.recordMilking = recordMilking;
    }

    @Transactional
    public Traite execute(Command command) {
        // Find the active lactation for this animal
        Lactation activeLactation = lactationRepository
                .findByAnimalIdAndTenantIdAndStatut(
                        command.animalId(),
                        command.tenantId(),
                        StatutLactation.EN_COURS
                )
                .orElseGet(() -> {
                    // Auto-create a lactation if none exists
                    Lactation newLactation = new Lactation(
                            command.tenantId(),
                            command.animalId(),
                            java.time.LocalDate.now(),
                            command.auteurId()
                    );
                    return lactationRepository.save(newLactation);
                });

        // Delegate to the standard milking command
        return recordMilking.execute(
                new RecordMilking.Command(
                        command.tenantId(),
                        activeLactation.getId(),
                        command.auteurId(),
                        command.dateHeure(),
                        command.type(),
                        command.quantiteKg()
                )
        );
    }

    public record Command(
            UUID tenantId,
            UUID animalId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            BigDecimal quantiteKg
    ) {
    }
}
