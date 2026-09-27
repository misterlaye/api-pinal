package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.application.AuthService;
import com.dairy.apipinal.identity.application.OtpService;
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
                otpService, jwtTokenProvider, utilisateurRepository, refreshTokenRepository
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
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn("access-token-123");
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
        when(jwtTokenProvider.generateAccessToken(any(UUID.class))).thenReturn("access-token");
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

        assertThrows(IllegalArgumentException.class,
                () -> authService.verifyOtp("+221770001122", "000000"));
    }

    @Test
    void shouldRefreshTokenWithRotation() throws Exception {
        UUID userId = UUID.randomUUID();
        String rawRefreshToken = "old-refresh-token";
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken existing = new RefreshToken(tokenHash, userId, OffsetDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash))
                .thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn("new-access-token");
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

        assertThrows(IllegalArgumentException.class,
                () -> authService.refreshToken("invalid-token"));
    }

    @Test
    void shouldLogoutByRevokingRefreshToken() throws Exception {
        String rawRefreshToken = "refresh-to-revoke";
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken existing = new RefreshToken(tokenHash, UUID.randomUUID(), OffsetDateTime.now().plusDays(7));
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash))
                .thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.logout(rawRefreshToken);

        assertTrue(existing.isRevoked());
        verify(refreshTokenRepository).save(existing);
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(value.getBytes());
        return HexFormat.of().formatHex(hashBytes);
    }
}
