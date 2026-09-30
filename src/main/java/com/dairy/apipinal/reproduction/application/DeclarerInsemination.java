package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.domain.MethodeReproduction;
import com.dairy.apipinal.reproduction.domain.StatutReproduction;
import com.dairy.apipinal.reproduction.domain.VelageDateCalculatorPolicy;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DeclarerInsemination {

    private final CycleReproductionRepository repository;
    private final VelageDateCalculatorPolicy dateCalculatorPolicy;
    private final AnimalQueries animalQueries;

    public DeclarerInsemination(
            CycleReproductionRepository repository,
            VelageDateCalculatorPolicy dateCalculatorPolicy,
            AnimalQueries animalQueries
    ) {
        this.repository = repository;
        this.dateCalculatorPolicy = dateCalculatorPolicy;
        this.animalQueries = animalQueries;
    }

    @Transactional
    public CycleReproduction execute(Command command) {

        if (animalQueries != null) {
            AnimalReference animal = animalQueries.getReference(command.animalId());
            if (!"ACTIF".equals(animal.statut())) {
                throw new IllegalStateException("Un animal non actif ne peut pas être inséminé.");
            }
            if (animal.sexe() != null && !"FEMELLE".equalsIgnoreCase(animal.sexe())) {
                throw new IllegalStateException("Seule une femelle peut être inséminée.");
            }
            if (!animal.exploitationId().equals(command.exploitationId())) {
                throw new IllegalArgumentException("L'animal n'appartient pas à cette exploitation.");
            }
        }


        List<CycleReproduction> cycles = repository.findByAnimalIdOrderByNumeroCycleDesc(command.animalId());

        if (!cycles.isEmpty()) {
            CycleReproduction dernierCycle = cycles.get(0);
            if (dernierCycle.getStatut() == StatutReproduction.EN_ATTENTE_CONSTAT ||
                dernierCycle.getStatut() == StatutReproduction.GESTANTE) {
                throw new IllegalStateException("L'animal a déjà un cycle en cours (" + dernierCycle.getStatut() + ").");
            }
        }

        int numeroCycle = cycles.isEmpty() ? 1 : cycles.get(0).getNumeroCycle() + 1;

        LocalDate datePrevueVelage = dateCalculatorPolicy.calculate(command.dateInsemination(), command.methodeReproduction());

        CycleReproduction cycle = new CycleReproduction(
                command.animalId(),
                command.tenantId(),
                command.exploitationId(),
                numeroCycle,
                command.dateInsemination(),
                command.methodeReproduction(),
                command.taureauId(),
                command.codePaillette(),
                datePrevueVelage,
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
            UUID taureauId,
            String codePaillette,
            UUID actorId
    ) {
    }
}
