package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.health.infrastructure.persistence.EvenementSanitaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListAnimalHealthEvents {

    private final EvenementSanitaireRepository repository;

    public ListAnimalHealthEvents(
            EvenementSanitaireRepository repository
    ) {
        this.repository = repository;
    }

    public List<EvenementSanitaire> execute(UUID tenantId,UUID animalId) {
        return repository.findAllByAnimalIdAndTenantIdOrderByDateHeureDesc(animalId, tenantId);
    }
}