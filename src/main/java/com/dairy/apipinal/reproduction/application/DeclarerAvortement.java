package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DeclarerAvortement {

    private final CycleReproductionRepository repository;

    public DeclarerAvortement(CycleReproductionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CycleReproduction execute(Command command) {
        CycleReproduction cycle = repository.findById(command.cycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle de reproduction introuvable."));

        cycle.declarerAvortement(command.actorId());

        return repository.save(cycle);
    }

    public record Command(
            UUID cycleId,
            UUID actorId
    ) {
    }
}
