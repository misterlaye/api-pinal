package com.dairy.apipinal.identity.application;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Génère et vérifie les codes PIN ouvriers.
 * Les PIN sont tirés avec un générateur cryptographique et conservés sous forme d'empreinte BCrypt.
 */
@Component
public class PinCodeService {

    private static final int PIN_LENGTH = 6;
    private static final int PIN_BOUND = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * Empreinte d'un PIN arbitraire, utilisée pour garder un temps de réponse comparable
     * lorsque l'utilisateur est inconnu (évite de révéler l'existence d'un compte).
     */
    private final String dummyHash = encoder.encode("000000");

    public String generatePin() {
        return String.format("%0" + PIN_LENGTH + "d", secureRandom.nextInt(PIN_BOUND));
    }

    public String hash(String pin) {
        return encoder.encode(pin);
    }

    public boolean matches(String pin, String pinHash) {
        if (pin == null || pinHash == null) {
            return false;
        }
        return encoder.matches(pin, pinHash);
    }

    public void simulateVerification(String pin) {
        encoder.matches(pin == null ? "" : pin, dummyHash);
    }
}
