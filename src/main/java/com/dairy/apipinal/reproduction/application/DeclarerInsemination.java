package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.domain.MethodeReproduction;
import com.dairy.apipinal.reproduction.domain.StatutReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DeclarerInsemination {

    private final CycleReproductionRepository repository;

    public DeclarerInsemination(CycleReproductionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CycleReproduction execute(Command command) {

        List<CycleReproduction> cycles = repository.findByAnimalIdOrderByNumeroCycleDesc(command.animalId());

        if (!cycles.isEmpty()) {
            CycleReproduction dernierCycle = cycles.get(0);
            if (dernierCycle.getStatut() == StatutReproduction.EN_ATTENTE_CONSTAT ||
                dernierCycle.getStatut() == StatutReproduction.GESTANTE) {
                throw new IllegalStateException("L'animal a déjà un cycle en cours (" + dernierCycle.getStatut() + ").");
            }
        }

        int numeroCycle = cycles.isEmpty() ? 1 : cycles.get(0).getNumeroCycle() + 1;

        CycleReproduction cycle = new CycleReproduction(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                numeroCycle,
                command.dateInsemination(),
                command.methodeReproduction(),
                command.identifiantTaureau(),
                command.actorId()
        );

        return repository.save(cycle);
    }

    public record Command(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            LocalDate dateInsemination,
            MethodeReproduction methodeReproduction,
            String identifiantTaureau,
            UUID actorId
    ) {
    }
}
