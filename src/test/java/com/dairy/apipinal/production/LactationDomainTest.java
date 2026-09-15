package com.dairy.apipinal.production;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LactationDomainTest {

    @Test
    void newLactationShouldBeActive() {

        Lactation lactation = new Lactation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 10),
                UUID.randomUUID()
        );

        assertEquals(
                StatutLactation.EN_COURS,
                lactation.getStatut()
        );
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {

        Lactation lactation = new Lactation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.of(2026, 1, 10),
                UUID.randomUUID()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> lactation.terminer(
                        LocalDate.of(2026, 1, 9),
                        UUID.randomUUID()
                )
        );
    }
}