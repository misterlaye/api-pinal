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

@Service
@Transactional(readOnly = true)
public class IdentityQueriesImpl implements IdentityQueries {

    private final MembershipRepository membershipRepository;
    private final ExploitationRepository exploitationRepository;

    public IdentityQueriesImpl(
            MembershipRepository membershipRepository,
            ExploitationRepository exploitationRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.exploitationRepository = exploitationRepository;
    }

    @Override
    public List<ExploitationInfo> getUserExploitations(UUID utilisateurId) {
        List<Membership> memberships = membershipRepository.findByUtilisateurId(utilisateurId);

        return memberships.stream()
                .map(membership -> exploitationRepository.findById(membership.getExploitationId())
                        .filter(Exploitation::isActif)
                        .map(exp -> new ExploitationInfo(
                                exp.getId(),
                                exp.getTenantId(),
                                exp.getNom(),
                                exp.getLocalite(),
                                membership.getRole().name()
                        )))
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
                            .map(exp -> new ExploitationInfo(
                                    exp.getId(),
                                    exp.getTenantId(),
                                    exp.getNom(),
                                    exp.getLocalite(),
                                    membership.getRole().name()
                            )));
        }

        return getUserExploitations(utilisateurId).stream().findFirst();
    }
}
