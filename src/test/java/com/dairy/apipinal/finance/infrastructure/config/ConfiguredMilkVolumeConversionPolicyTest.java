package com.dairy.apipinal.finance.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredMilkVolumeConversionPolicyTest {

    private Environment environment;
    private ConfiguredMilkVolumeConversionPolicy policy;

    @BeforeEach
    void setUp() {
        environment = Mockito.mock(Environment.class);
        policy = new ConfiguredMilkVolumeConversionPolicy(environment);
    }

    @Test
    void shouldConvertKgToLitresAccordingToConfiguredMultiplier() {
        // Given
        Mockito.when(environment.getProperty("finance.lait.litres-par-kg"))
                .thenReturn("0.971"); // Sémantique : 1 kg = 0.971 litres

        BigDecimal volumeKg = new BigDecimal("100.0");

        // When
        BigDecimal volumeLitres = policy.convertKgToLitres(volumeKg);

        // Then
        // 100 kg * 0.971 = 97.100 litres
        assertThat(volumeLitres).isEqualByComparingTo(new BigDecimal("97.1"));
    }

    @Test
    void shouldThrowExceptionWhenPropertyIsMissing() {
        // Given
        Mockito.when(environment.getProperty("finance.lait.litres-par-kg"))
                .thenReturn(null);

        BigDecimal volumeKg = new BigDecimal("100.0");

        // When/Then
        assertThatThrownBy(() -> policy.convertKgToLitres(volumeKg))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("n'est pas configuré");
    }
}
