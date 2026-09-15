package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.api.HealthEventReference;
import com.dairy.apipinal.health.api.HealthQueries;
import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.health.infrastructure.persistence.EvenementSanitaireRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class HealthQueriesImpl implements HealthQueries {

    private final EvenementSanitaireRepository repository;
    private final TenantContext tenantContext;

    public HealthQueriesImpl(
            EvenementSanitaireRepository repository,
            TenantContext tenantContext
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    @Override
    public HealthEventReference getReference(UUID healthEventId) {

        return repository
                .findByIdAndTenantId(
                        healthEventId,
                        tenantContext.currentTenantId()
                )
                .map(this::toReference)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Événement sanitaire introuvable."
                ));
    }

    @Override
    public List<HealthEventReference> getAnimalHistory(
            UUID animalId
    ) {
        return repository
                .findAllByAnimalIdAndTenantIdOrderByDateHeureDesc(
                        animalId,
                        tenantContext.currentTenantId()
                )
                .stream()
                .map(this::toReference)
                .toList();
    }

    private HealthEventReference toReference(
            EvenementSanitaire event
    ) {
        return new HealthEventReference(
                event.getId(),
                event.getAnimalId(),
                event.getDateHeure(),
                event.getDescription(),
                event.getDiagnostic(),
                event.getTraitement(),
                event.getDateFin()
        );
    }
}