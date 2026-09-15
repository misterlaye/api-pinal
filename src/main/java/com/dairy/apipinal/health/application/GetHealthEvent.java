package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.health.infrastructure.persistence.EvenementSanitaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetHealthEvent {

    private final EvenementSanitaireRepository repository;

    public GetHealthEvent(
            EvenementSanitaireRepository repository
    ) {
        this.repository = repository;
    }

    public EvenementSanitaire execute(UUID tenantId, UUID eventId) {
        return repository
                .findByIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Événement sanitaire introuvable."));
    }
}