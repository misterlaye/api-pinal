package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RationTest {

    private static final UUID ANIMAL_ID = UUID.randomUUID();

    @Test
    void shouldStartAsDraft() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        assertThat(ration.getStatut())
                .isEqualTo(StatutRation.BROUILLON);
    }

    @Test
    void shouldRejectEmptyRationActivation() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        assertThatThrownBy(ration::activer)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAddLine() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        UUID alimentId = UUID.randomUUID();

        ration.ajouterLigne(
                alimentId,
                new BigDecimal("5.0000")
        );

        assertThat(ration.getLignes())
                .hasSize(1);
    }

    @Test
    void shouldRejectDuplicateFeed() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        UUID alimentId = UUID.randomUUID();

        ration.ajouterLigne(
                alimentId,
                new BigDecimal("5")
        );

        assertThatThrownBy(() ->
                ration.ajouterLigne(
                        alimentId,
                        new BigDecimal("2")
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        assertThatThrownBy(() ->
                ration.ajouterLigne(
                        UUID.randomUUID(),
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldActivateRationWithAtLeastOneLine() {
        Ration ration = new Ration(
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5")
        );

        ration.activer();

        assertThat(ration.getStatut())
                .isEqualTo(StatutRation.ACTIVE);
    }
}