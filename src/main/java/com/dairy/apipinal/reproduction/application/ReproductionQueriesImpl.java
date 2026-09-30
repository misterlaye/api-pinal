package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.reproduction.api.CycleReproductionResponse;
import com.dairy.apipinal.reproduction.api.ReproductionQueries;
import com.dairy.apipinal.reproduction.domain.ConstatGestation;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReproductionQueriesImpl implements ReproductionQueries {

    private final CycleReproductionRepository repository;

    public ReproductionQueriesImpl(CycleReproductionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CycleReproductionResponse> getCyclesByAnimal(UUID animalId) {
        return repository.findByAnimalIdOrderByNumeroCycleDesc(animalId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private CycleReproductionResponse mapToResponse(CycleReproduction cycle) {
        List<CycleReproductionResponse.ConstatGestationResponse> constats = cycle.getConstatsGestation()
                .stream()
                .map(c -> new CycleReproductionResponse.ConstatGestationResponse(c.getDate(), c.getResultat(), c.getVeterinaire()))
                .collect(Collectors.toList());

        return new CycleReproductionResponse(
                cycle.getId(),
                cycle.getAnimalId(),
                cycle.getNumeroCycle(),
                cycle.getStatut().name(),
                cycle.getDateInsemination(),
                cycle.getMethodeReproduction().name(),
                cycle.getTaureauId(),
                cycle.getCodePaillette(),
                cycle.getDatePrevueVelage(),
                cycle.getDateReelleVelage(),
                constats,
                cycle.getCreatedAt() != null ? cycle.getCreatedAt() : null
        );
    }
}
