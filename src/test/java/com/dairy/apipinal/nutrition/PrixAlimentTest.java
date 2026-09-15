package com.dairy.apipinal.nutrition.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrixAlimentTest {

    @Test
    void shouldRejectZeroPrice() {
        assertThatThrownBy(() ->
                new PrixAliment(
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        LocalDate.of(2026, 1, 1),
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThatThrownBy(() ->
                new PrixAliment(
                        UUID.randomUUID(),
                        new BigDecimal("-10"),
                        LocalDate.of(2026, 1, 1),
                        null
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectInvalidPeriod() {
        assertThatThrownBy(() ->
                new PrixAliment(
                        UUID.randomUUID(),
                        new BigDecimal("250"),
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1)
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }
}