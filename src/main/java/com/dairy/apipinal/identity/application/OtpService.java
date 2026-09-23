package com.dairy.apipinal.identity.application;

import com.dairy.apipinal.identity.domain.OtpCode;
import com.dairy.apipinal.identity.infrastructure.persistence.OtpCodeRepository;
import com.dairy.apipinal.identity.infrastructure.sms.SmsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * Gère la génération, l'envoi et la vérification des codes OTP.
 */
@Service
@Transactional
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final SmsProvider smsProvider;
    private final int otpExpirationSeconds;
    private final int maxAttempts;
    private final int codeLength;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            SmsProvider smsProvider,
            @Value("${pinal.otp.expiration}") int otpExpirationSeconds,
            @Value("${pinal.otp.max-attempts}") int maxAttempts,
            @Value("${pinal.otp.code-length}") int codeLength
    ) {
        this.otpCodeRepository = otpCodeRepository;
        this.smsProvider = smsProvider;
        this.otpExpirationSeconds = otpExpirationSeconds;
        this.maxAttempts = maxAttempts;
        this.codeLength = codeLength;
    }

    /**
     * Génère un code OTP, le stocke en BDD (hashé) et l'envoie par SMS.
     */
    public void generateAndSend(String telephone) {
        String code = generateCode();
        String codeHash = hash(code);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(otpExpirationSeconds);

        OtpCode otpCode = new OtpCode(telephone, codeHash, expiresAt);
        otpCodeRepository.save(otpCode);

        smsProvider.sendSms(telephone, "Votre code Pinal : " + code);
    }

    /**
     * Vérifie un code OTP soumis par l'utilisateur.
     * @return true si le code est valide, false sinon.
     * @throws IllegalStateException si le code est expiré ou le nombre max de tentatives atteint.
     */
    public boolean verify(String telephone, String code) {
        OtpCode otpCode = otpCodeRepository
                .findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(telephone)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun code OTP actif pour ce numéro."
                ));

        if (otpCode.isExpired()) {
            throw new IllegalStateException("Le code OTP a expiré.");
        }

        if (otpCode.getAttempts() >= maxAttempts) {
            throw new IllegalStateException(
                    "Nombre maximum de tentatives atteint. Veuillez demander un nouveau code."
            );
        }

        otpCode.incrementAttempts();

        String submittedHash = hash(code);
        if (submittedHash.equals(otpCode.getCodeHash())) {
            otpCode.markUsed();
            otpCodeRepository.save(otpCode);
            return true;
        }

        otpCodeRepository.save(otpCode);
        return false;
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, codeLength);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + codeLength + "d", code);
    }

    String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }
}
