package com.dairy.apipinal.identity.infrastructure.security;

import com.dairy.apipinal.shared.security.AuthenticationMethod;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Génère et valide les JSON Web Tokens (access tokens) pour l'authentification.
 * Implémente TokenValidator pour permettre la validation depuis le module shared.
 * Le claim {@code amr} porte le moyen d'authentification (OTP ou PIN).
 */
@Component
public class JwtTokenProvider implements TokenValidator {

    static final String AUTH_METHOD_CLAIM = "amr";

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
     * Génère un access token JWT pour un utilisateur authentifié par OTP.
     */
    public String generateAccessToken(UUID userId) {
        return generateAccessToken(userId, AuthenticationMethod.OTP);
    }

    /**
     * Génère un access token JWT en précisant le moyen d'authentification.
     */
    public String generateAccessToken(UUID userId, AuthenticationMethod method) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpirationSeconds * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(AUTH_METHOD_CLAIM, method.name())
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
     * Valide un access token et retourne l'utilisateur et le moyen d'authentification.
     * Un jeton sans claim {@code amr} est traité comme une session PIN (la plus restreinte).
     */
    @Override
    public Optional<ValidatedToken> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String method = claims.get(AUTH_METHOD_CLAIM, String.class);
            AuthenticationMethod authenticationMethod = AuthenticationMethod.OTP.name().equals(method)
                    ? AuthenticationMethod.OTP
                    : AuthenticationMethod.PIN;
            return Optional.of(new ValidatedToken(userId, authenticationMethod));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
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
