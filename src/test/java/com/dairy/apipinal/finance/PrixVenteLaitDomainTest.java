package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrixVenteLaitDomainTest {

    @Test
    void shouldAcceptPositivePrice() {

        assertDoesNotThrow(() ->
                new PrixVenteLait(
                        UUID.randomUUID(),
                        new BigDecimal("575.00"),
                        LocalDate.of(2026, 1, 1),
                        null
                )
        );
    }

    @Test
    void shouldRejectZeroPrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrixVenteLait(
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        LocalDate.of(2026, 1, 1),
                        null
                )
        );
    }

    @Test
    void shouldRejectNegativePrice() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrixVenteLait(
                        UUID.randomUUID(),
                        new BigDecimal("-10.00"),
                        LocalDate.of(2026, 1, 1),
                        null
                )
        );
    }

    @Test
    void shouldRejectInvalidPeriod() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PrixVenteLait(
                        UUID.randomUUID(),
                        new BigDecimal("575.00"),
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1)
                )
        );
    }
}