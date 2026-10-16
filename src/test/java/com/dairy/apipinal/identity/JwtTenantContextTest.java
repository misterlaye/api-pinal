package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.identity.domain.RoleExploitation;
import com.dairy.apipinal.identity.infrastructure.security.JwtTenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTenantContextTest {

    @Mock
    private IdentityQueries identityQueries;

    private JwtTenantContext jwtTenantContext;

    @BeforeEach
    void setUp() {
        jwtTenantContext = new JwtTenantContext(identityQueries);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractUserIdAndTenantIdFromAuthentication() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID expId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ExploitationInfo info = new ExploitationInfo(expId, tenantId, "Ferme Pinal", "Dakar", com.dairy.apipinal.identity.domain.RoleExploitation.PROPRIETAIRE, 0, "Bon");
        when(identityQueries.getActiveExploitationForUser(userId, Optional.empty()))
                .thenReturn(Optional.of(info));

        assertEquals(userId, jwtTenantContext.currentUserId());
        assertEquals(tenantId, jwtTenantContext.currentTenantId());
    }

    @Test
    void shouldResolveExploitationOncePerRequestAndNotAcrossRequests() {
        UUID userId = UUID.randomUUID();
        UUID expA = UUID.randomUUID();
        UUID expB = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
        ExploitationInfo infoA = new ExploitationInfo(expA, UUID.randomUUID(), "A", "Dakar", RoleExploitation.PROPRIETAIRE, 0, "Bon");
        ExploitationInfo infoB = new ExploitationInfo(expB, UUID.randomUUID(), "B", "Thies", RoleExploitation.EMPLOYE, 0, "Bon");
        when(identityQueries.getActiveExploitationForUser(userId, Optional.of(expA))).thenReturn(Optional.of(infoA));
        when(identityQueries.getActiveExploitationForUser(userId, Optional.of(expB))).thenReturn(Optional.of(infoB));

        try {
            startRequest(expA);
            assertEquals(infoA.tenantId(), jwtTenantContext.currentTenantId());
            assertEquals(expA, jwtTenantContext.currentExploitationId());
            assertEquals("PROPRIETAIRE", jwtTenantContext.currentRole());
            verify(identityQueries, times(1)).getActiveExploitationForUser(userId, Optional.of(expA));

            startRequest(expB);
            assertEquals(expB, jwtTenantContext.currentExploitationId());
            assertEquals("EMPLOYE", jwtTenantContext.currentRole());

            startRequest(expA);
            jwtTenantContext.currentTenantId();
            verify(identityQueries, times(2)).getActiveExploitationForUser(userId, Optional.of(expA));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldNotCacheFailedResolution() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
        when(identityQueries.getActiveExploitationForUser(userId, Optional.empty())).thenReturn(Optional.empty());

        try {
            startRequest(null);
            assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentTenantId());
            assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentTenantId());
            verify(identityQueries, times(2)).getActiveExploitationForUser(userId, Optional.empty());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    private static void startRequest(UUID exploitationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (exploitationId != null) {
            request.addHeader("X-Exploitation-ID", exploitationId.toString());
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentUserId());
        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentTenantId());
    }
}
