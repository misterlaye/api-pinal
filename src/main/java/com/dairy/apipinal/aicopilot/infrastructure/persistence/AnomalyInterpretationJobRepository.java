package com.dairy.apipinal.aicopilot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AnomalyInterpretationJobRepository extends JpaRepository<AnomalyInterpretationJob, UUID> {

    Optional<AnomalyInterpretationJob> findByTenantIdAndExploitationIdAndAnomalyIdAndAnalysisFingerprint(
            UUID tenantId, UUID exploitationId, String anomalyId, String analysisFingerprint
    );

    Optional<AnomalyInterpretationJob> findByIdAndTenantIdAndExploitationId(
            UUID id, UUID tenantId, UUID exploitationId
    );

    Optional<AnomalyInterpretationJob> findFirstByTenantIdAndExploitationIdAndAnomalyIdOrderByCreatedAtDesc(
            UUID tenantId, UUID exploitationId, String anomalyId
    );
}
