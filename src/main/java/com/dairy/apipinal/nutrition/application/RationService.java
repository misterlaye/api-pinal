package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RationService {

    private final RationRepository rationRepository;
    private final AlimentRepository alimentRepository;
    private final AnimalQueries animalQueries;
    private final TenantContext tenantContext;

    public RationService(
            RationRepository rationRepository,
            AlimentRepository alimentRepository,
            AnimalQueries animalQueries,
            TenantContext tenantContext
    ) {
        this.rationRepository = rationRepository;
        this.alimentRepository = alimentRepository;
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

    @Transactional
    public Ration addLine(AddRationLine command) {

        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(
                        command.rationId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + command.rationId()
                        )
                );

        if (!ration.getAnimalId().equals(command.animalId())) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        alimentRepository
                .findByIdAndActifTrue(command.alimentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aliment introuvable ou inactif : "
                                        + command.alimentId()
                        )
                );

        ration.ajouterLigne(
                command.alimentId(),
                command.quantite()
        );

        return rationRepository.save(ration);
    }
}