package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "my-super-secret-key-for-development-only-min-32-chars!!",
                900,   // 15 minutes
                604800 // 7 jours
        );
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.generateAccessToken(userId);

        assertNotNull(token);
        assertFalse(token.isBlank());

        UUID extracted = jwtTokenProvider.validateTokenAndGetUserId(token);
        assertEquals(userId, extracted);
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        UUID result = jwtTokenProvider.validateTokenAndGetUserId("token-invalide");
        assertNull(result);
    }

    @Test
    void shouldReturnNullForTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.generateAccessToken(userId);

        // Altérer le token
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        UUID result = jwtTokenProvider.validateTokenAndGetUserId(tampered);
        assertNull(result);
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "une-autre-cle-secrete-de-32-caracteres-minimum!!",
                900, 604800
        );

        UUID userId = UUID.randomUUID();
        String token = otherProvider.generateAccessToken(userId);

        UUID result = jwtTokenProvider.validateTokenAndGetUserId(token);
        assertNull(result);
    }

    @Test
    void shouldGenerateUniqueRefreshTokens() {
        String token1 = jwtTokenProvider.generateRefreshToken();
        String token2 = jwtTokenProvider.generateRefreshToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    void shouldReturnCorrectExpirationValues() {
        assertEquals(900, jwtTokenProvider.getAccessTokenExpirationSeconds());
        assertEquals(604800, jwtTokenProvider.getRefreshTokenExpirationSeconds());
    }
}
