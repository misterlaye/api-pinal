package com.dairy.apipinal.animal.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.animal.domain.Animal;
import com.dairy.apipinal.animal.domain.StatutAnimal;
import com.dairy.apipinal.animal.infrastructure.persistence.AnimalRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnimalQueriesImpl implements AnimalQueries {

    private final AnimalRepository animalRepository;
    private final TenantContext tenantContext;

    public AnimalQueriesImpl(
            AnimalRepository animalRepository,
            TenantContext tenantContext
    ) {
        this.animalRepository = animalRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public AnimalReference getReference(UUID animalId) {

        Animal animal = animalRepository
                .findByIdAndTenantId(
                        animalId,
                        tenantContext.currentTenantId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Animal introuvable."
                ));

        return toReference(animal);
    }

    @Override
    public boolean exists(UUID animalId) {

        return animalRepository
                .findByIdAndTenantId(
                        animalId,
                        tenantContext.currentTenantId()
                )
                .isPresent();
    }

    @Override
    public boolean isActive(UUID animalId) {

        return animalRepository.existsByIdAndTenantIdAndStatut(
                animalId,
                tenantContext.currentTenantId(),
                StatutAnimal.ACTIF
        );
    }

    @Override
    public boolean isEligibleForProduction(UUID animalId) {
        return isActive(animalId);
    }

    private AnimalReference toReference(Animal animal) {

        return new AnimalReference(
                animal.getId(),
                animal.getTenantId(),
                animal.getExploitationId(),
                animal.getRaceId(),
                animal.getIdentifiant(),
                animal.getNom(),
                animal.getStatut().name()
        );
    }
}