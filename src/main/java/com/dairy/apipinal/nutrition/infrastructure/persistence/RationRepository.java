package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.Ration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RationRepository
        extends JpaRepository<Ration, UUID> {

    Optional<Ration> findByIdAndTenantId(
            UUID id,
            UUID tenantId
    );

    boolean existsByIdAndTenantId(
            UUID id,
            UUID tenantId
    );
}