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

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ANIMAL_ID = UUID.randomUUID();

    private Ration createRation() {
        return new Ration(
                TENANT_ID,
                ANIMAL_ID,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );
    }

    @Test
    void shouldStartAsDraft() {
        Ration ration = createRation();

        assertThat(ration.getTenantId())
                .isEqualTo(TENANT_ID);

        assertThat(ration.getAnimalId())
                .isEqualTo(ANIMAL_ID);

        assertThat(ration.getStatut())
                .isEqualTo(StatutRation.BROUILLON);
    }

    @Test
    void shouldRejectEmptyRationActivation() {
        Ration ration = createRation();

        assertThatThrownBy(ration::activer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Une ration doit contenir au moins une ligne avant activation."
                );
    }

    @Test
    void shouldAddLine() {
        Ration ration = createRation();

        UUID alimentId = UUID.randomUUID();

        ration.ajouterLigne(
                alimentId,
                new BigDecimal("5.0000")
        );

        assertThat(ration.getLignes())
                .hasSize(1);

        assertThat(ration.getLignes().getFirst().getAlimentId())
                .isEqualTo(alimentId);

        assertThat(ration.getLignes().getFirst().getQuantite())
                .isEqualByComparingTo("5.0000");
    }

    @Test
    void shouldRejectDuplicateFeed() {
        Ration ration = createRation();

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Un aliment ne peut apparaître qu'une seule fois dans une ration."
                );
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        Ration ration = createRation();

        assertThatThrownBy(() ->
                ration.ajouterLigne(
                        UUID.randomUUID(),
                        BigDecimal.ZERO
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "La quantité doit être strictement positive."
                );
    }

    @Test
    void shouldRejectNegativeQuantity() {
        Ration ration = createRation();

        assertThatThrownBy(() ->
                ration.ajouterLigne(
                        UUID.randomUUID(),
                        new BigDecimal("-1")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "La quantité doit être strictement positive."
                );
    }

    @Test
    void shouldActivateDraftRationWithAtLeastOneLine() {
        Ration ration = createRation();

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5")
        );

        ration.activer();

        assertThat(ration.getStatut())
                .isEqualTo(StatutRation.ACTIVE);
    }

    @Test
    void shouldRejectActivationOfAlreadyActiveRation() {
        Ration ration = createRation();

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5")
        );

        ration.activer();

        assertThatThrownBy(ration::activer)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Seule une ration brouillon peut être activée."
                );
    }

    @Test
    void shouldTerminateActiveRation() {
        Ration ration = createRation();

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5")
        );

        ration.activer();

        LocalDate dateFin = LocalDate.of(2026, 9, 15);

        ration.terminer(dateFin);

        assertThat(ration.getStatut())
                .isEqualTo(StatutRation.TERMINEE);

        assertThat(ration.getDateFin())
                .isEqualTo(dateFin);
    }

    @Test
    void shouldRejectTerminationOfDraftRation() {
        Ration ration = createRation();

        assertThatThrownBy(() ->
                ration.terminer(
                        LocalDate.of(2026, 9, 15)
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Seule une ration active peut être terminée."
                );
    }

    @Test
    void shouldRejectTerminationBeforeStartDate() {
        Ration ration = createRation();

        ration.ajouterLigne(
                UUID.randomUUID(),
                new BigDecimal("5")
        );

        ration.activer();

        assertThatThrownBy(() ->
                ration.terminer(
                        LocalDate.of(2026, 8, 31)
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "La date de fin ne peut pas être antérieure au début."
                );
    }
}