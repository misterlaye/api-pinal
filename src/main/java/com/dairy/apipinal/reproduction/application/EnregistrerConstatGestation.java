package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class EnregistrerConstatGestation {

    private final CycleReproductionRepository repository;

    public EnregistrerConstatGestation(CycleReproductionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CycleReproduction execute(Command command) {
        CycleReproduction cycle = repository.findById(command.cycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle de reproduction introuvable."));

        cycle.enregistrerConstat(
                command.dateConstat(),
                command.resultat(),
                command.veterinaire(),
                command.actorId()
        );

        return repository.save(cycle);
    }

    public record Command(
            UUID cycleId,
            LocalDate dateConstat,
            String resultat,
            String veterinaire,
            UUID actorId
    ) {
    }
}
