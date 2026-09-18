package com.dairy.apipinal.identity.infrastructure.persistence;

import com.dairy.apipinal.identity.domain.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUtilisateurId(UUID utilisateurId);

    Optional<Membership> findByUtilisateurIdAndExploitationId(UUID utilisateurId, UUID exploitationId);
}
