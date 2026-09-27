package com.dairy.apipinal.identity.infrastructure.security;

import com.dairy.apipinal.shared.security.TokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Génère et valide les JSON Web Tokens (access tokens) pour l'authentification.
 * Implémente TokenValidator pour permettre la validation depuis le module shared.
 */
@Component
public class JwtTokenProvider implements TokenValidator {

    private final SecretKey secretKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${pinal.jwt.secret}") String secret,
            @Value("${pinal.jwt.access-token-expiration}") long accessTokenExpirationSeconds,
            @Value("${pinal.jwt.refresh-token-expiration}") long refreshTokenExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    /**
     * Génère un access token JWT pour un utilisateur.
     */
    public String generateAccessToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Génère un refresh token opaque (UUID).
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Valide un access token et retourne le userId (subject).
     * @return le UUID du userId, ou null si le token est invalide.
     */
    @Override
    public UUID validateTokenAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Retourne la durée d'expiration de l'access token en secondes.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    /**
     * Retourne la durée d'expiration du refresh token en secondes.
     */
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }
}
