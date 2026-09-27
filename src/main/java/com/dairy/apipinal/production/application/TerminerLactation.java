package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class TerminerLactation {

    private final LactationRepository lactationRepository;

    public TerminerLactation(LactationRepository lactationRepository) {
        this.lactationRepository = lactationRepository;
    }

    @Transactional
    public Lactation execute(Command command) {
        Lactation activeLactation = lactationRepository
                .findByAnimalIdAndTenantIdAndStatut(
                        command.animalId(),
                        command.tenantId(),
                        StatutLactation.EN_COURS
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Impossible de tarir cette vache : aucune lactation en cours trouvée."
                ));

        activeLactation.terminer(command.dateFin(), command.actorId());

        return lactationRepository.save(activeLactation);
    }

    public record Command(
            UUID tenantId,
            UUID animalId,
            LocalDate dateFin,
            UUID actorId
    ) {
    }
}
