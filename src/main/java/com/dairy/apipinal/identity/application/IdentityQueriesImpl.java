package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.api.ExploitationAccess;
import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.identity.domain.Exploitation;
import com.dairy.apipinal.identity.domain.Membership;
import com.dairy.apipinal.identity.domain.RoleExploitation;
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
                                            "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIF'",
                                            Integer.class, exp.getTenantId(), exp.getId()
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
                                                "SELECT COUNT(*) FROM animal WHERE tenant_id = ? AND exploitation_id = ? AND statut = 'ACTIF'",
                                                Integer.class, exp.getTenantId(), exp.getId()
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

    @Override
    public Optional<ExploitationAccess> resolveAccess(
            UUID utilisateurId,
            Optional<UUID> requestedExploitationId,
            boolean workerRolesOnly
    ) {
        if (requestedExploitationId.isPresent()) {
            UUID expId = requestedExploitationId.get();
            return membershipRepository
                    .findByUtilisateurIdAndExploitationId(utilisateurId, expId)
                    .filter(membership -> !workerRolesOnly || isWorkerRole(membership.getRole()))
                    .flatMap(membership -> exploitationRepository.findById(expId)
                            .filter(Exploitation::isActif)
                            .map(exp -> new ExploitationAccess(exp.getId(), exp.getTenantId(), membership.getRole())));
        }

        List<ExploitationAccess> eligible = membershipRepository.findByUtilisateurId(utilisateurId).stream()
                .filter(membership -> !workerRolesOnly || isWorkerRole(membership.getRole()))
                .map(membership -> exploitationRepository.findById(membership.getExploitationId())
                        .filter(Exploitation::isActif)
                        .map(exp -> new ExploitationAccess(exp.getId(), exp.getTenantId(), membership.getRole())))
                .flatMap(Optional::stream)
                .toList();

        // Sans exploitation explicite, on ne choisit jamais arbitrairement parmi plusieurs exploitations.
        return eligible.size() == 1 ? Optional.of(eligible.getFirst()) : Optional.empty();
    }

    private static boolean isWorkerRole(RoleExploitation role) {
        return role == RoleExploitation.EMPLOYE || role == RoleExploitation.GERANT;
    }
}
