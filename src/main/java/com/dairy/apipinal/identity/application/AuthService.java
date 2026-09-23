package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.RefreshToken;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.domain.StatutUtilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.RefreshTokenRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Orchestre le flux d'authentification : OTP → tokens → refresh → logout.
 */
@Service
@Transactional
public class AuthService {

    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(
            OtpService otpService,
            JwtTokenProvider jwtTokenProvider,
            UtilisateurRepository utilisateurRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.otpService = otpService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.utilisateurRepository = utilisateurRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Envoie un code OTP au numéro de téléphone.
     */
    public void requestOtp(String telephone) {
        if (telephone == null || telephone.isBlank()) {
            throw new IllegalArgumentException("Le numéro de téléphone est obligatoire.");
        }
        otpService.generateAndSend(telephone);
    }

    /**
     * Vérifie le code OTP et retourne les tokens d'authentification.
     * Crée l'utilisateur s'il n'existe pas encore (auto-inscription).
     */
    public AuthResponse verifyOtp(String telephone, String code) {
        boolean valid = otpService.verify(telephone, code);
        if (!valid) {
            throw new IllegalArgumentException("Code OTP invalide.");
        }

        Utilisateur user = utilisateurRepository.findByTelephone(telephone)
                .orElseGet(() -> {
                    Utilisateur newUser = new Utilisateur(
                            UUID.randomUUID(),
                            telephone,
                            "Nom",
                            "Prénom"
                    );
                    return utilisateurRepository.save(newUser);
                });

        return generateTokens(user.getId());
    }

    /**
     * Rafraîchit les tokens en vérifiant le refresh token existant.
     * Applique la rotation : l'ancien refresh token est révoqué, un nouveau est émis.
     */
    public AuthResponse refreshToken(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalide ou révoqué."));

        if (existing.isExpired()) {
            existing.revoke();
            refreshTokenRepository.save(existing);
            throw new IllegalStateException("Refresh token expiré.");
        }

        // Rotation : révoquer l'ancien
        existing.revoke();
        refreshTokenRepository.save(existing);

        return generateTokens(existing.getUserId());
    }

    /**
     * Révoque un refresh token (déconnexion côté serveur).
     */
    public void logout(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse generateTokens(UUID userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        String refreshTokenHash = hash(rawRefreshToken);

        OffsetDateTime refreshExpiresAt = OffsetDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = new RefreshToken(refreshTokenHash, userId, refreshExpiresAt);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                userId
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    /**
     * Réponse d'authentification contenant les tokens.
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UUID userId
    ) {}
}
