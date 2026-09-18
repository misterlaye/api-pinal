package com.dairy.apipinal.identity.infrastructure.persistence;

import com.dairy.apipinal.identity.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
