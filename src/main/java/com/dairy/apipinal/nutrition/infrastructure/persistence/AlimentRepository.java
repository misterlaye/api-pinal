package com.dairy.apipinal.nutrition.infrastructure.persistence;

import com.dairy.apipinal.nutrition.domain.Aliment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlimentRepository extends JpaRepository<Aliment, UUID> {

    boolean existsByExploitationIdAndCode(UUID exploitationId, String code);

    List<Aliment> findAllByTenantIdAndExploitationIdOrderByNomAsc(UUID tenantId, UUID exploitationId);

    Optional<Aliment> findByIdAndTenantIdAndExploitationId(UUID id, UUID tenantId, UUID exploitationId);

    Optional<Aliment> findByIdAndTenantIdAndExploitationIdAndActifTrue(UUID id, UUID tenantId, UUID exploitationId);


}