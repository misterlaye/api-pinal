package com.dairy.apipinal.identity.infrastructure.web;

import com.dairy.apipinal.identity.application.AuthService;
import com.dairy.apipinal.identity.infrastructure.web.dto.OtpRequest;
import com.dairy.apipinal.identity.infrastructure.web.dto.OtpVerifyRequest;
import com.dairy.apipinal.identity.infrastructure.web.dto.RefreshRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur REST public pour l'authentification :
 * - Envoi OTP
 * - Vérification OTP → tokens
 * - Rafraîchissement de tokens
 * - Déconnexion (révocation refresh token)
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(
            @Valid @RequestBody OtpRequest request
    ) {
        authService.requestOtp(request.telephone());
        return ResponseEntity.ok(Map.of(
                "message", "Code OTP envoyé au " + request.telephone()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthService.AuthResponse> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request
    ) {
        AuthService.AuthResponse response = authService.verifyOtp(
                request.telephone(),
                request.code()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthService.AuthResponse> refreshToken(
            @Valid @RequestBody RefreshRequest request
    ) {
        AuthService.AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshRequest request
    ) {
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie."));
    }
}
