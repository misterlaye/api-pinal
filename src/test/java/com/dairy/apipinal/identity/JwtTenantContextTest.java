package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.api.ExploitationAccess;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.identity.domain.RoleExploitation;
import com.dairy.apipinal.identity.infrastructure.security.JwtTenantContext;
import com.dairy.apipinal.shared.security.AuthenticationMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldExtractUserIdAndTenantIdFromAuthentication() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        authenticate(userId, AuthenticationMethod.OTP);

        when(identityQueries.resolveAccess(userId, Optional.empty(), false))
                .thenReturn(Optional.of(new ExploitationAccess(expId, tenantId, RoleExploitation.PROPRIETAIRE)));

        assertEquals(userId, jwtTenantContext.currentUserId());
        assertEquals(tenantId, jwtTenantContext.currentTenantId());
        assertEquals(AuthenticationMethod.OTP, jwtTenantContext.currentAuthenticationMethod());
    }

    @Test
    void shouldResolveExploitationOncePerRequestAndNotAcrossRequests() {
        UUID userId = UUID.randomUUID();
        UUID expA = UUID.randomUUID();
        UUID expB = UUID.randomUUID();
        authenticate(userId, AuthenticationMethod.OTP);
        ExploitationAccess accessA = new ExploitationAccess(expA, UUID.randomUUID(), RoleExploitation.PROPRIETAIRE);
        ExploitationAccess accessB = new ExploitationAccess(expB, UUID.randomUUID(), RoleExploitation.EMPLOYE);
        when(identityQueries.resolveAccess(userId, Optional.of(expA), false)).thenReturn(Optional.of(accessA));
        when(identityQueries.resolveAccess(userId, Optional.of(expB), false)).thenReturn(Optional.of(accessB));

        startRequest(expA);
        assertEquals(accessA.tenantId(), jwtTenantContext.currentTenantId());
        assertEquals(expA, jwtTenantContext.currentExploitationId());
        assertEquals("PROPRIETAIRE", jwtTenantContext.currentRole());
        verify(identityQueries, times(1)).resolveAccess(userId, Optional.of(expA), false);

        startRequest(expB);
        assertEquals(expB, jwtTenantContext.currentExploitationId());
        assertEquals("EMPLOYE", jwtTenantContext.currentRole());

        startRequest(expA);
        jwtTenantContext.currentTenantId();
        verify(identityQueries, times(2)).resolveAccess(userId, Optional.of(expA), false);
    }

    @Test
    void shouldDenyAndNotCacheFailedResolution() {
        UUID userId = UUID.randomUUID();
        authenticate(userId, AuthenticationMethod.OTP);
        when(identityQueries.resolveAccess(userId, Optional.empty(), false)).thenReturn(Optional.empty());

        startRequest(null);
        assertThrows(AccessDeniedException.class, () -> jwtTenantContext.currentTenantId());
        assertThrows(AccessDeniedException.class, () -> jwtTenantContext.currentTenantId());
        verify(identityQueries, times(2)).resolveAccess(userId, Optional.empty(), false);
    }

    @Test
    void pinSessionOnlyResolvesWorkerMemberships() {
        UUID userId = UUID.randomUUID();
        UUID exp = UUID.randomUUID();
        authenticate(userId, AuthenticationMethod.PIN);
        when(identityQueries.resolveAccess(userId, Optional.of(exp), true)).thenReturn(Optional.empty());

        startRequest(exp);
        assertThrows(AccessDeniedException.class, () -> jwtTenantContext.currentRole());
        verify(identityQueries).resolveAccess(userId, Optional.of(exp), true);
    }

    @Test
    void requireExploitationRejectsAnotherExploitation() {
        UUID userId = UUID.randomUUID();
        UUID exp = UUID.randomUUID();
        authenticate(userId, AuthenticationMethod.OTP);
        when(identityQueries.resolveAccess(userId, Optional.of(exp), false))
                .thenReturn(Optional.of(new ExploitationAccess(exp, UUID.randomUUID(), RoleExploitation.PROPRIETAIRE)));

        startRequest(exp);
        assertEquals(exp, jwtTenantContext.requireExploitation(null));
        assertEquals(exp, jwtTenantContext.requireExploitation(exp));
        assertThrows(AccessDeniedException.class, () -> jwtTenantContext.requireExploitation(UUID.randomUUID()));
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentUserId());
        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentTenantId());
    }

    private static void authenticate(UUID userId, AuthenticationMethod method) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        auth.setDetails(method);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static void startRequest(UUID exploitationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (exploitationId != null) {
            request.addHeader("X-Exploitation-ID", exploitationId.toString());
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
