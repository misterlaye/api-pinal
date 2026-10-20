package com.dairy.apipinal.finance.infrastructure.persistence;

import com.dairy.apipinal.finance.domain.CalculRentabilite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Snapshots de rentabilité, propres à chaque exploitation.
 */
public interface CalculRentabiliteRepository
        extends JpaRepository<CalculRentabilite, UUID> {

    Optional<CalculRentabilite>
    findFirstByTenantIdAndExploitationIdOrderByDateCalculDesc(UUID tenantId, UUID exploitationId);

    List<CalculRentabilite>
    findAllByTenantIdAndExploitationIdOrderByDateCalculDesc(UUID tenantId, UUID exploitationId);

    /**
     * Les 4 snapshots les plus récents de l'exploitation (optimisation du tableau de bord financier).
     */
    List<CalculRentabilite>
    findTop4ByTenantIdAndExploitationIdOrderByDateCalculDesc(UUID tenantId, UUID exploitationId);
}
