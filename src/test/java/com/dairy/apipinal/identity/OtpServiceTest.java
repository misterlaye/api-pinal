package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.application.OtpService;
import com.dairy.apipinal.identity.domain.OtpCode;
import com.dairy.apipinal.identity.infrastructure.persistence.OtpCodeRepository;
import com.dairy.apipinal.identity.infrastructure.sms.SmsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private SmsProvider smsProvider;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpCodeRepository, smsProvider, 300, 5, 6);
    }

    @Test
    void shouldGenerateAndSendOtp() {
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(i -> i.getArgument(0));

        otpService.generateAndSend("+221770001122");

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(captor.capture());
        verify(smsProvider).sendSms(eq("+221770001122"), any(String.class));

        OtpCode saved = captor.getValue();
        assertEquals("+221770001122", saved.getTelephone());
        assertNotNull(saved.getCodeHash());
        assertFalse(saved.isUsed());
        assertEquals(0, saved.getAttempts());
    }

    @Test
    void shouldVerifyValidCode() {
        String telephone = "+221770001122";
        String code = "123456";
        String codeHash = sha256(code);

        OtpCode otpCode = new OtpCode(telephone, codeHash, OffsetDateTime.now().plusMinutes(5));
        when(otpCodeRepository.findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(telephone))
                .thenReturn(Optional.of(otpCode));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(i -> i.getArgument(0));

        boolean result = otpService.verify(telephone, code);

        assertTrue(result);
        assertTrue(otpCode.isUsed());
    }

    @Test
    void shouldRejectInvalidCode() {
        String telephone = "+221770001122";
        String codeHash = sha256("123456");

        OtpCode otpCode = new OtpCode(telephone, codeHash, OffsetDateTime.now().plusMinutes(5));
        when(otpCodeRepository.findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(telephone))
                .thenReturn(Optional.of(otpCode));
        when(otpCodeRepository.save(any(OtpCode.class))).thenAnswer(i -> i.getArgument(0));

        boolean result = otpService.verify(telephone, "999999");

        assertFalse(result);
        assertFalse(otpCode.isUsed());
        assertEquals(1, otpCode.getAttempts());
    }

    @Test
    void shouldThrowWhenExpired() {
        String telephone = "+221770001122";
        String codeHash = sha256("123456");

        OtpCode otpCode = new OtpCode(telephone, codeHash, OffsetDateTime.now().minusMinutes(1));
        when(otpCodeRepository.findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(telephone))
                .thenReturn(Optional.of(otpCode));

        assertThrows(IllegalStateException.class,
                () -> otpService.verify(telephone, "123456"));
    }

    @Test
    void shouldThrowWhenMaxAttemptsReached() {
        String telephone = "+221770001122";
        String codeHash = sha256("123456");

        OtpCode otpCode = new OtpCode(telephone, codeHash, OffsetDateTime.now().plusMinutes(5));
        for (int i = 0; i < 5; i++) {
            otpCode.incrementAttempts();
        }

        when(otpCodeRepository.findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc(telephone))
                .thenReturn(Optional.of(otpCode));

        assertThrows(IllegalStateException.class,
                () -> otpService.verify(telephone, "123456"));
    }

    @Test
    void shouldThrowWhenNoActiveOtp() {
        when(otpCodeRepository.findFirstByTelephoneAndUsedFalseOrderByCreatedAtDesc("+221770001122"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> otpService.verify("+221770001122", "123456"));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

