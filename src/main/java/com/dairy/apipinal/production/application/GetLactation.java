package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLactation {

    private final LactationRepository lactationRepository;

    public GetLactation(LactationRepository lactationRepository) {
        this.lactationRepository = lactationRepository;
    }

    public Lactation execute(UUID tenantId, UUID lactationId) {

        return lactationRepository
                .findByIdAndTenantId(lactationId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lactation introuvable."
                ));
    }
}