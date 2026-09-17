package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.RationCostReference;
import com.dairy.apipinal.nutrition.api.RationReference;
import com.dairy.apipinal.nutrition.application.CalculateRationCost;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class NutritionQueriesImpl implements NutritionQueries {

    private final RationRepository rationRepository;
    private final RationService rationService;
    private final TenantContext tenantContext;

    public NutritionQueriesImpl(
            RationRepository rationRepository,
            RationService rationService,
            TenantContext tenantContext
    ) {
        this.rationRepository = rationRepository;
        this.rationService = rationService;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional(readOnly = true)
    public RationReference getRation(UUID rationId) {

        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(rationId, tenantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + rationId
                        )
                );

        return toReference(ration);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID rationId) {

        UUID tenantId = tenantContext.currentTenantId();

        return rationRepository.existsByIdAndTenantId(
                rationId,
                tenantId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RationReference> findActiveRation(
            UUID animalId,
            LocalDate date
    ) {
        UUID tenantId = tenantContext.currentTenantId();

        return rationRepository
                .findActiveRationAtDate(
                        tenantId,
                        animalId,
                        date,
                        StatutRation.ACTIVE
                )
                .map(this::toReference);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RationCostReference> calculateFeedCost(
            UUID animalId,
            LocalDate date
    ) {
        Optional<RationReference> ration =
                findActiveRation(animalId, date);

        if (ration.isEmpty()) {
            return Optional.empty();
        }

        var result = rationService.calculateCost(
                new CalculateRationCost(
                        animalId,
                        ration.get().id(),
                        date
                )
        );

        return Optional.of(
                new RationCostReference(
                        result.rationId(),
                        animalId,
                        date,
                        result.coutTotal()
                )
        );
    }

    private RationReference toReference(Ration ration) {
        return new RationReference(
                ration.getId(),
                ration.getTenantId(),
                ration.getAnimalId(),
                ration.getDateDebut(),
                ration.getDateFin(),
                ration.getStatut(),
                ration.getOrigine()
        );
    }
}