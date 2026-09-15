package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.production.api.ProductionReference;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductionQueriesImpl implements ProductionQueries {

    private final LactationRepository lactationRepository;
    private final TenantContext tenantContext;

    public ProductionQueriesImpl(
            LactationRepository lactationRepository,
            TenantContext tenantContext
    ) {
        this.lactationRepository = lactationRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public ProductionReference getActiveLactation(UUID animalId) {

        return lactationRepository
                .findByAnimalIdAndTenantIdAndStatut(
                        animalId,
                        tenantContext.currentTenantId(),
                        StatutLactation.EN_COURS
                )
                .map(lactation -> new ProductionReference(
                        lactation.getId(),
                        lactation.getAnimalId(),
                        lactation.getDateDebut(),
                        lactation.getDateFin(),
                        lactation.getStatut()
                ))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune lactation active pour cet animal."
                ));
    }
}