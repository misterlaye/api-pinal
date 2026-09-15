package com.dairy.apipinal.health;

import com.dairy.apipinal.health.domain.EvenementSanitaire;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HealthDomainTest {

    @Test
    void shouldRequireDescription() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new EvenementSanitaire(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.now(),
                        "",
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldRejectInvalidEndDate() {

        EvenementSanitaire event =
                new EvenementSanitaire(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.parse(
                                "2026-01-10T10:00:00Z"
                        ),
                        "Suspicion de problème sanitaire",
                        UUID.randomUUID()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> event.update(
                        "Suspicion de problème sanitaire",
                        null,
                        null,
                        LocalDate.of(2026, 1, 9),
                        UUID.randomUUID()
                )
        );
    }

    @Test
    void shouldAllowDiagnosticToBeAddedLater() {

        EvenementSanitaire event =
                new EvenementSanitaire(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        OffsetDateTime.parse(
                                "2026-01-10T10:00:00Z"
                        ),
                        "Suspicion de problème sanitaire",
                        UUID.randomUUID()
                );

        event.update(
                "Suspicion de problème sanitaire",
                "Mammite confirmée",
                "Traitement vétérinaire",
                LocalDate.of(2026, 1, 15),
                UUID.randomUUID()
        );

        assertEquals(
                "Mammite confirmée",
                event.getDiagnostic()
        );

        assertEquals(
                "Traitement vétérinaire",
                event.getTraitement()
        );
    }
}