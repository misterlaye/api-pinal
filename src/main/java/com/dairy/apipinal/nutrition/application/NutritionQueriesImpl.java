package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.RationReference;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NutritionQueriesImpl implements NutritionQueries {

    private final RationRepository rationRepository;
    private final TenantContext tenantContext;

    public NutritionQueriesImpl(
            RationRepository rationRepository,
            TenantContext tenantContext
    ) {
        this.rationRepository = rationRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public RationReference getRation(UUID rationId) {

        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(rationId, tenantId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + rationId
                        )
                );

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

    @Override
    public boolean exists(UUID rationId) {

        UUID tenantId = tenantContext.currentTenantId();

        return rationRepository.existsByIdAndTenantId(
                rationId,
                tenantId
        );
    }
}