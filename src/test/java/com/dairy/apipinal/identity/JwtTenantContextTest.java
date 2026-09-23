package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.api.ExploitationInfo;
import com.dairy.apipinal.identity.api.IdentityQueries;
import com.dairy.apipinal.identity.infrastructure.security.JwtTenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        UUID expId = UUID.randomUUID();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        ExploitationInfo info = new ExploitationInfo(expId, tenantId, "Ferme Pinal", "Dakar", "PROPRIETAIRE");
        when(identityQueries.getActiveExploitationForUser(userId, Optional.empty()))
                .thenReturn(Optional.of(info));

        assertEquals(userId, jwtTenantContext.currentUserId());
        assertEquals(tenantId, jwtTenantContext.currentTenantId());
    }

    @Test
    void shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentUserId());
        assertThrows(IllegalStateException.class, () -> jwtTenantContext.currentTenantId());
    }
}
