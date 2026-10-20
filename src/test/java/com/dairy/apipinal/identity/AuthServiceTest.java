package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.application.AuthService;
import com.dairy.apipinal.identity.application.OtpService;
import com.dairy.apipinal.identity.application.PinCodeService;
import com.dairy.apipinal.identity.domain.RefreshToken;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.RefreshTokenRepository;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import com.dairy.apipinal.identity.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OtpService otpService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                otpService, new PinCodeService(), jwtTokenProvider, utilisateurRepository, refreshTokenRepository,
                5, java.time.Duration.ofMinutes(15)
        );
    }

    @Test
    void shouldRequestOtp() {
        authService.requestOtp("+221770001122");
        verify(otpService).generateAndSend("+221770001122");
    }

    @Test
    void shouldThrowWhenRequestOtpWithBlankPhone() {
        assertThrows(IllegalArgumentException.class, () -> authService.requestOtp(""));
        assertThrows(IllegalArgumentException.class, () -> authService.requestOtp(null));
    }

    @Test
    void shouldVerifyOtpAndReturnTokensForExistingUser() {
        String telephone = "+221770001122";
        UUID userId = UUID.randomUUID();
        Utilisateur user = new Utilisateur(userId, telephone, "Diallo", "Abdoulaye");

        when(otpService.verify(telephone, "123456")).thenReturn(true);
        when(utilisateurRepository.findByTelephone(telephone)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userId, com.dairy.apipinal.shared.security.AuthenticationMethod.OTP)).thenReturn("access-token-123");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token-456");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthService.AuthResponse response = authService.verifyOtp(telephone, "123456");

        assertNotNull(response);
        assertEquals("access-token-123", response.accessToken());
        assertEquals("refresh-token-456", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        assertEquals(userId, response.userId());
    }

    @Test
    void shouldVerifyOtpAndAutoRegisterNewUser() {
        String telephone = "+221770001122";

        when(otpService.verify(telephone, "123456")).thenReturn(true);
        when(utilisateurRepository.findByTelephone(telephone)).thenReturn(Optional.empty());
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(UUID.class), eq(com.dairy.apipinal.shared.security.AuthenticationMethod.OTP))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthService.AuthResponse response = authService.verifyOtp(telephone, "123456");

        assertNotNull(response);
        verify(utilisateurRepository).save(any(Utilisateur.class));
    }

    @Test
    void shouldThrowWhenOtpInvalid() {
        when(otpService.verify("+221770001122", "000000")).thenReturn(false);

        assertThrows(com.dairy.apipinal.identity.application.AuthenticationFailedException.class,
                () -> authService.verifyOtp("+221770001122", "000000"));
    }

    @Test
    void shouldRefreshTokenWithRotation() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "old-refresh-token";
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken existing = new RefreshToken(tokenHash, userId, OffsetDateTime.now().plusDays(7), com.dairy.apipinal.shared.security.AuthenticationMethod.OTP);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash))
                .thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(userId, com.dairy.apipinal.shared.security.AuthenticationMethod.OTP)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.AuthResponse response = authService.refreshToken(rawRefreshToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertTrue(existing.isRevoked()); // L'ancien est révoqué
    }

    @Test
    void shouldThrowWhenRefreshTokenInvalid() {
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
                .thenReturn(Optional.empty());

        assertThrows(com.dairy.apipinal.identity.application.AuthenticationFailedException.class,
                () -> authService.refreshToken("invalid-token"));
    }

    @Test
    void shouldLogoutByRevokingRefreshToken() throws Exception {
        String rawRefreshToken = "refresh-to-revoke";
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken existing = new RefreshToken(tokenHash, UUID.randomUUID(), OffsetDateTime.now().plusDays(7), com.dairy.apipinal.shared.security.AuthenticationMethod.OTP);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash))
                .thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.logout(rawRefreshToken);

        assertTrue(existing.isRevoked());
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void shouldKeepPinMethodWhenRefreshingPinSession() throws Exception {
        UUID userId = UUID.randomUUID();
        String tokenHash = sha256("pin-refresh");
        RefreshToken existing = new RefreshToken(tokenHash, userId, OffsetDateTime.now().plusDays(7), com.dairy.apipinal.shared.security.AuthenticationMethod.PIN);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(userId, com.dairy.apipinal.shared.security.AuthenticationMethod.PIN)).thenReturn("pin-access");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("pin-refresh-2");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.AuthResponse response = authService.refreshToken("pin-refresh");

        assertEquals("pin-access", response.accessToken());
        verify(refreshTokenRepository).save(argThat(token -> token != existing && token.getAuthMethod() == com.dairy.apipinal.shared.security.AuthenticationMethod.PIN));
    }

    @Test
    void shouldRejectPinForUserWithoutPinWithGenericMessage() {
        Utilisateur owner = new Utilisateur(UUID.randomUUID(), "+221770009999", "Diallo", "Awa");
        when(utilisateurRepository.findByTelephone("+221770009999")).thenReturn(Optional.of(owner));
        when(utilisateurRepository.findByTelephone("+221770000000")).thenReturn(Optional.empty());

        var noPin = assertThrows(com.dairy.apipinal.identity.application.AuthenticationFailedException.class,
                () -> authService.verifyPin("+221770009999", "123456"));
        var unknown = assertThrows(com.dairy.apipinal.identity.application.AuthenticationFailedException.class,
                () -> authService.verifyPin("+221770000000", "123456"));
        assertEquals(unknown.getMessage(), noPin.getMessage());
    }

    @Test
    void shouldLockPinAfterMaxFailedAttempts() {
        PinCodeService pins = new PinCodeService();
        Utilisateur worker = new Utilisateur(UUID.randomUUID(), "+221770001111", "Ba", "Moussa");
        worker.definirPinHash(pins.hash("246810"));
        when(utilisateurRepository.findByTelephone("+221770001111")).thenReturn(Optional.of(worker));

        for (int i = 0; i < 4; i++) {
            assertThrows(com.dairy.apipinal.identity.application.AuthenticationFailedException.class,
                    () -> authService.verifyPin("+221770001111", "000000"));
        }
        assertThrows(com.dairy.apipinal.identity.application.TooManyAttemptsException.class,
                () -> authService.verifyPin("+221770001111", "000000"));
        assertTrue(worker.isPinVerrouille(OffsetDateTime.now()));
        assertThrows(com.dairy.apipinal.identity.application.TooManyAttemptsException.class,
                () -> authService.verifyPin("+221770001111", "246810"));
    }

    @Test
    void shouldOpenPinSessionWithCorrectPin() {
        PinCodeService pins = new PinCodeService();
        UUID userId = UUID.randomUUID();
        Utilisateur worker = new Utilisateur(userId, "+221770002222", "Ba", "Moussa");
        worker.definirPinHash(pins.hash("135790"));
        when(utilisateurRepository.findByTelephone("+221770002222")).thenReturn(Optional.of(worker));
        when(jwtTokenProvider.generateAccessToken(userId, com.dairy.apipinal.shared.security.AuthenticationMethod.PIN)).thenReturn("pin-token");
        when(jwtTokenProvider.generateRefreshToken()).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);

        AuthService.AuthResponse response = authService.verifyPin("+221770002222", "135790");

        assertEquals("pin-token", response.accessToken());
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(value.getBytes());
        return HexFormat.of().formatHex(hashBytes);
    }
}
