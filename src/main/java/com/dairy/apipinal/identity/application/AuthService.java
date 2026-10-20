package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.RefreshToken;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.RefreshTokenRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import com.dairy.apipinal.shared.security.AuthenticationMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre le flux d'authentification : OTP ou PIN → tokens → refresh → logout.
 * <p>
 * Les échecs d'authentification sont persistés (compteurs OTP et PIN) : les exceptions
 * {@link AuthenticationFailedException} et {@link TooManyAttemptsException} ne provoquent pas de rollback.
 */
@Service
@Transactional
public class AuthService {

    static final String INVALID_PIN_MESSAGE = "Numéro de téléphone ou code PIN invalide.";

    private final OtpService otpService;
    private final PinCodeService pinCodeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UtilisateurRepository utilisateurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final int pinMaxAttempts;
    private final Duration pinLockDuration;

    public AuthService(
            OtpService otpService,
            PinCodeService pinCodeService,
            JwtTokenProvider jwtTokenProvider,
            UtilisateurRepository utilisateurRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${pinal.pin.max-attempts:5}") int pinMaxAttempts,
            @Value("${pinal.pin.lock-duration:PT15M}") Duration pinLockDuration
    ) {
        this.otpService = otpService;
        this.pinCodeService = pinCodeService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.utilisateurRepository = utilisateurRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.pinMaxAttempts = pinMaxAttempts;
        this.pinLockDuration = pinLockDuration;
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
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, TooManyAttemptsException.class})
    public AuthResponse verifyOtp(String telephone, String code) {
        boolean valid = otpService.verify(telephone, code);
        if (!valid) {
            throw new AuthenticationFailedException("Code invalide. Vérifiez le code reçu par SMS.");
        }

        Utilisateur user = utilisateurRepository.findByTelephone(telephone)
                .orElseGet(() -> utilisateurRepository.save(new Utilisateur(
                        UUID.randomUUID(),
                        telephone,
                        "Nom",
                        "Prénom"
                )));

        return generateTokens(user.getId(), AuthenticationMethod.OTP);
    }

    /**
     * Vérifie le PIN permanent d'un ouvrier et retourne des tokens de session PIN.
     * Après {@code pinal.pin.max-attempts} échecs consécutifs, le PIN est verrouillé
     * pendant {@code pinal.pin.lock-duration}.
     */
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, TooManyAttemptsException.class})
    public AuthResponse verifyPin(String telephone, String pin) {
        if (telephone == null || telephone.isBlank() || pin == null || pin.isBlank()) {
            throw new AuthenticationFailedException(INVALID_PIN_MESSAGE);
        }

        Optional<Utilisateur> found = utilisateurRepository.findByTelephone(telephone)
                .filter(Utilisateur::hasPin);
        if (found.isEmpty()) {
            pinCodeService.simulateVerification(pin);
            throw new AuthenticationFailedException(INVALID_PIN_MESSAGE);
        }

        Utilisateur user = found.get();
        OffsetDateTime now = OffsetDateTime.now();
        if (user.isPinVerrouille(now)) {
            throw lockedException(user, now);
        }

        if (!pinCodeService.matches(pin, user.getPinHash())) {
            user.enregistrerEchecPin(now, pinMaxAttempts, pinLockDuration);
            utilisateurRepository.save(user);
            if (user.isPinVerrouille(now)) {
                throw lockedException(user, now);
            }
            throw new AuthenticationFailedException(INVALID_PIN_MESSAGE);
        }

        user.reinitialiserEchecsPin();
        utilisateurRepository.save(user);
        return generateTokens(user.getId(), AuthenticationMethod.PIN);
    }

    /**
     * Rafraîchit les tokens en vérifiant le refresh token existant.
     * Applique la rotation : l'ancien refresh token est révoqué, un nouveau est émis
     * avec le même moyen d'authentification.
     */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public AuthResponse refreshToken(String rawRefreshToken) {
        String tokenHash = hash(rawRefreshToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException("Session expirée. Veuillez vous reconnecter."));

        if (existing.isExpired()) {
            existing.revoke();
            refreshTokenRepository.save(existing);
            throw new AuthenticationFailedException("Session expirée. Veuillez vous reconnecter.");
        }

        existing.revoke();
        refreshTokenRepository.save(existing);

        return generateTokens(existing.getUserId(), existing.getAuthMethod());
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

    private TooManyAttemptsException lockedException(Utilisateur user, OffsetDateTime now) {
        long minutes = Math.max(1, Duration.between(now, user.getPinLockedUntil()).toMinutes() + 1);
        return new TooManyAttemptsException(
                "Trop de tentatives. Code PIN bloqué pendant " + minutes + " minute(s)."
        );
    }

    private AuthResponse generateTokens(UUID userId, AuthenticationMethod method) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId, method);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        String refreshTokenHash = hash(rawRefreshToken);

        OffsetDateTime refreshExpiresAt = OffsetDateTime.now()
                .plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = new RefreshToken(refreshTokenHash, userId, refreshExpiresAt, method);
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
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
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
