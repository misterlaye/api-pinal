package com.dairy.apipinal.production;

import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TraiteDomainTest {

    @Test
    void shouldRejectNegativeQuantity() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Traite(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.now(),
                        TypeTraite.MATIN,
                        new BigDecimal("-1"),
                        OffsetDateTime.now()
                )
        );
    }

    @Test
    void shouldAcceptZeroQuantity() {

        Traite traite = new Traite(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                OffsetDateTime.now(),
                TypeTraite.MATIN,
                BigDecimal.ZERO,
                OffsetDateTime.now()
        );

        assertEquals(
                BigDecimal.ZERO,
                traite.getQuantiteKg()
        );
    }
}