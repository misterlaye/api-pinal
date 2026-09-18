package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.application.IdentityQueriesImpl;
import com.dairy.apipinal.identity.domain.Exploitation;
import com.dairy.apipinal.identity.domain.Membership;
import com.dairy.apipinal.identity.domain.RoleExploitation;
import com.dairy.apipinal.identity.infrastructure.persistence.ExploitationRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityQueriesTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private ExploitationRepository exploitationRepository;

    private IdentityQueriesImpl identityQueries;

    @BeforeEach
    void setUp() {
        identityQueries = new IdentityQueriesImpl(membershipRepository, exploitationRepository);
    }

    @Test
    void shouldReturnEmptyWhenUserHasNoMemberships() {
        UUID userId = UUID.randomUUID();

        when(membershipRepository.findByUtilisateurId(userId))
                .thenReturn(Collections.emptyList());

        List<ExploitationInfo> list = identityQueries.getUserExploitations(userId);
        assertTrue(list.isEmpty());

        Optional<ExploitationInfo> active = identityQueries.getActiveExploitationForUser(userId, Optional.empty());
        assertTrue(active.isEmpty());
    }

    @Test
    void shouldReturnActiveExploitationForUser() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();

        Membership membership = new Membership(userId, expId, RoleExploitation.PROPRIETAIRE);
        Exploitation exploitation = new Exploitation(tenantId, "Ferme Pinal", "Dakar");

        when(membershipRepository.findByUtilisateurId(userId))
                .thenReturn(List.of(membership));
        when(exploitationRepository.findById(expId))
                .thenReturn(Optional.of(exploitation));

        Optional<ExploitationInfo> active = identityQueries.getActiveExploitationForUser(userId, Optional.empty());

        assertTrue(active.isPresent());
        assertEquals(tenantId, active.get().tenantId());
        assertEquals("Ferme Pinal", active.get().nom());
        assertEquals("PROPRIETAIRE", active.get().role());
    }

    @Test
    void shouldReturnRequestedExploitationWhenUserHasMembership() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();

        Membership membership = new Membership(userId, expId, RoleExploitation.GERANT);
        Exploitation exploitation = new Exploitation(tenantId, "Ferme 2", "Thies");

        when(membershipRepository.findByUtilisateurIdAndExploitationId(userId, expId))
                .thenReturn(Optional.of(membership));
        when(exploitationRepository.findById(expId))
                .thenReturn(Optional.of(exploitation));

        Optional<ExploitationInfo> result = identityQueries.getActiveExploitationForUser(userId, Optional.of(expId));

        assertTrue(result.isPresent());
        assertEquals(tenantId, result.get().tenantId());
        assertEquals("GERANT", result.get().role());
    }
}
