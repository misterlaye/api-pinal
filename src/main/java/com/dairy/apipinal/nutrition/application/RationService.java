package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.RationAlreadyActiveException;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.data.domain.Page;
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

    @Transactional
    public Ration activate(
            UUID animalId,
            UUID rationId
    ) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(
                        rationId,
                        tenantId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + rationId
                        )
                );

        if (!ration.getAnimalId().equals(animalId)) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        if (ration.getStatut() != StatutRation.BROUILLON) {
            throw new IllegalStateException(
                    "Seule une ration brouillon peut être activée."
            );
        }

        boolean overlapping;

        if (ration.getDateFin() == null) {
            overlapping =
                    rationRepository
                            .existsOverlappingActiveRationWithoutEndDate(
                                    tenantId,
                                    ration.getAnimalId(),
                                    ration.getDateDebut(),
                                    StatutRation.ACTIVE
                            );
        } else {
            overlapping =
                    rationRepository
                            .existsOverlappingActiveRationWithEndDate(
                                    tenantId,
                                    ration.getAnimalId(),
                                    ration.getDateDebut(),
                                    ration.getDateFin(),
                                    StatutRation.ACTIVE
                            );
        }

        if (overlapping) {
            throw new RationAlreadyActiveException();
        }

        ration.activer();

        return rationRepository.save(ration);
    }

    @Transactional
    public Ration terminate(TerminateRation command) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(command.rationId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ration introuvable."
                ));

        if (!ration.getAnimalId().equals(command.animalId())) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        ration.terminer(command.dateFin());

        return rationRepository.save(ration);
    }

    @Transactional(readOnly = true)
    public Ration getRation(GetRation query) {
        UUID tenantId = tenantContext.currentTenantId();

        return rationRepository
                .findByIdAndTenantId(query.rationId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ration introuvable."));
    }

    @Transactional(readOnly = true)
    public Page<Ration> getRationsByAnimal(GetRationsByAnimal query) {
        UUID tenantId = tenantContext.currentTenantId();

        animalQueries.getReference(query.animalId());

        return rationRepository.findByTenantIdAndAnimalId(
                tenantId,
                query.animalId(),
                query.pageable()
        );
    }
}