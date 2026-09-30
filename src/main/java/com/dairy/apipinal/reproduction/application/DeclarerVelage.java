package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.animal.api.NaissancesDeclareesEvent;
import com.dairy.apipinal.reproduction.api.VelageEnregistreEvent;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DeclarerVelage {

    private final CycleReproductionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DeclarerVelage(CycleReproductionRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CycleReproduction execute(Command command) {
        CycleReproduction cycle = repository.findById(command.cycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle de reproduction introuvable."));

        cycle.declarerVelage(command.dateReelle(), command.actorId());

        CycleReproduction saved = repository.save(cycle);

        eventPublisher.publishEvent(new VelageEnregistreEvent(
                saved.getId(),
                saved.getAnimalId(),
                saved.getTenantId(),
                saved.getExploitationId(),
                saved.getDateReelleVelage(),
                command.veaux(),
                saved.getTaureauId(),
                saved.getCodePaillette()
        ));

        if (command.veaux() != null && !command.veaux().isEmpty()) {
            eventPublisher.publishEvent(new NaissancesDeclareesEvent(
                    saved.getId(),
                    saved.getAnimalId(),
                    saved.getTenantId(),
                    saved.getExploitationId(),
                    saved.getDateReelleVelage(),
                    command.veaux().stream()
                            .map(veau -> new NaissancesDeclareesEvent.VeauPayload(
                                    veau.identifiant(), veau.nom(), veau.sexe(), veau.indexPortee()))
                            .toList(),
                    saved.getTaureauId(),
                    saved.getCodePaillette()
            ));
        }

        return saved;
    }

    public record Command(
            UUID cycleId,
            LocalDate dateReelle,
            List<VelageEnregistreEvent.VeauPayload> veaux,
            UUID actorId
    ) {
    }
}
