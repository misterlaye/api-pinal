package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RationService {

    private final RationRepository rationRepository;
    private final AnimalQueries animalQueries;
    private final TenantContext tenantContext;

    public RationService(
            RationRepository rationRepository,
            AnimalQueries animalQueries,
            TenantContext tenantContext
    ) {
        this.rationRepository = rationRepository;
        this.animalQueries = animalQueries;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public Ration create(CreateRation command) {

        UUID tenantId = tenantContext.currentTenantId();

        animalQueries.getReference(command.animalId());

        Ration ration = new Ration(
                tenantId,
                command.animalId(),
                command.dateDebut(),
                command.origine()
        );

        return rationRepository.save(ration);
    }
}