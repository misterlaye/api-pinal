package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.reproduction.api.AvortementDeclareEvent;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DeclarerAvortement {

    private final CycleReproductionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeclarerAvortement(CycleReproductionRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CycleReproduction execute(Command command) {
        CycleReproduction cycle = repository.findById(command.cycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle de reproduction introuvable."));

        cycle.declarerAvortement(command.actorId());

        CycleReproduction saved = repository.save(cycle);

        eventPublisher.publishEvent(new AvortementDeclareEvent(
                saved.getId(),
                saved.getAnimalId(),
                saved.getTenantId(),
                saved.getExploitationId(),
                LocalDate.now() // Date du jour pour l'avortement par défaut
        ));

        return saved;
    }

    public record Command(
            UUID cycleId,
            UUID actorId
    ) {
    }
}
