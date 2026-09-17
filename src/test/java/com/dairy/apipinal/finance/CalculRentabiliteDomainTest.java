package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.domain.CalculRentabilite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculRentabiliteDomainTest {

    @Test
    void shouldCalculateFinancialSnapshot() {

        CalculRentabilite calcul =
                CalculRentabilite.calculate(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("30.0000"),
                        new BigDecimal("20000.0000"),
                        new BigDecimal("5000.0000"),
                        new BigDecimal("2000.0000")
                );

        assertEquals(
                new BigDecimal("7000.0000"),
                calcul.getCoutTotal()
        );

        assertEquals(
                new BigDecimal("233.3333"),
                calcul.getCoutRevientParLitre()
        );

        assertEquals(
                new BigDecimal("13000.0000"),
                calcul.getMarge()
        );
    }

    @Test
    void shouldRejectZeroMilkVolume() {

        assertThrows(
                IllegalArgumentException.class,
                () -> CalculRentabilite.calculate(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        BigDecimal.ZERO,
                        new BigDecimal("20000"),
                        new BigDecimal("5000"),
                        new BigDecimal("2000")
                )
        );
    }

    @Test
    void shouldRejectInvalidPeriod() {

        assertThrows(
                IllegalArgumentException.class,
                () -> CalculRentabilite.calculate(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        LocalDate.of(2026, 9, 30),
                        LocalDate.of(2026, 9, 1),
                        new BigDecimal("30"),
                        new BigDecimal("20000"),
                        new BigDecimal("5000"),
                        new BigDecimal("2000")
                )
        );
    }

    @Test
    void shouldAllowNegativeMargin() {

        CalculRentabilite calcul =
                CalculRentabilite.calculate(
                        UUID.randomUUID(),
                        OffsetDateTime.now(ZoneOffset.UTC),
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 30),
                        new BigDecimal("30"),
                        new BigDecimal("5000"),
                        new BigDecimal("6000"),
                        new BigDecimal("1000")
                );

        assertEquals(
                new BigDecimal("-2000.0000"),
                calcul.getMarge()
        );
    }
}