package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.identity.domain.Exploitation;
import com.dairy.apipinal.identity.domain.Membership;
import com.dairy.apipinal.identity.infrastructure.persistence.ExploitationRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@Transactional(readOnly = true)
public class IdentityQueriesImpl implements IdentityQueries {

    private final MembershipRepository membershipRepository;
    private final ExploitationRepository exploitationRepository;
    private final JdbcTemplate jdbcTemplate;

    public IdentityQueriesImpl(
            MembershipRepository membershipRepository,
            ExploitationRepository exploitationRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.membershipRepository = membershipRepository;
        this.exploitationRepository = exploitationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ExploitationInfo> getUserExploitations(UUID utilisateurId) {
        List<Membership> memberships = membershipRepository.findByUtilisateurId(utilisateurId);

        return memberships.stream()
                .map(membership -> exploitationRepository.findById(membership.getExploitationId())
                        .filter(Exploitation::isActif)
                        .map(exp -> {
                                Integer count = null;
                                try {
                                    count = jdbcTemplate.queryForObject(
                                            "SELECT COUNT(*) FROM animal WHERE exploitation_id = ? AND statut = 'ACTIF'",
                                            Integer.class, exp.getId()
                                    );
                                } catch (Exception e) {}
                                int animalCount = count != null ? count : 0;
                                String status = "Bon";

                                return new ExploitationInfo(
                                        exp.getId(),
                                        exp.getTenantId(),
                                        exp.getNom(),
                                        exp.getLocalite(),
                                        membership.getRole(),
                                        animalCount,
                                        status
                                );
                        }))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<ExploitationInfo> getActiveExploitationForUser(
            UUID utilisateurId,
            Optional<UUID> requestedExploitationId
    ) {
        if (requestedExploitationId.isPresent()) {
            UUID expId = requestedExploitationId.get();
            return membershipRepository
                    .findByUtilisateurIdAndExploitationId(utilisateurId, expId)
                    .flatMap(membership -> exploitationRepository.findById(expId)
                            .filter(Exploitation::isActif)
                            .map(exp -> {
                                    Integer count = null;
                                    try {
                                        count = jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM animal WHERE exploitation_id = ? AND statut = 'ACTIF'",
                                                Integer.class, exp.getId()
                                        );
                                    } catch (Exception e) {}
                                    int animalCount = count != null ? count : 0;
                                    String status = "Bon";

                                    return new ExploitationInfo(
                                            exp.getId(),
                                            exp.getTenantId(),
                                            exp.getNom(),
                                            exp.getLocalite(),
                                            membership.getRole(),
                                            animalCount,
                                            status
                                    );
                            }));
        }

        return getUserExploitations(utilisateurId).stream().findFirst();
    }
}
