package com.dairy.apipinal.production.infrastructure.persistence;

import com.dairy.apipinal.production.domain.Traite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TraiteRepository extends JpaRepository<Traite, UUID> {

    List<Traite> findAllByLactationIdAndTenantIdOrderByDateHeureAsc(
            UUID lactationId,
            UUID tenantId
    );
}