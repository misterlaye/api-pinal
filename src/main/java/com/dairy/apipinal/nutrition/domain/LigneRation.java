package com.dairy.apipinal.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ligne_ration")
public class LigneRation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ration_id",
            nullable = false
    )
    private Ration ration;

    @Column(name = "aliment_id", nullable = false)
    private UUID alimentId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantite;

    protected LigneRation() {
    }

    LigneRation(
            Ration ration,
            UUID alimentId,
            BigDecimal quantite
    ) {
        this.ration = Objects.requireNonNull(
                ration,
                "La ration est obligatoire."
        );

        this.alimentId = Objects.requireNonNull(
                alimentId,
                "L'aliment est obligatoire."
        );

        this.quantite = requirePositive(quantite);
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        Objects.requireNonNull(
                value,
                "La quantité est obligatoire."
        );

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(
                    "La quantité doit être strictement positive."
            );
        }

        return value;
    }

    public UUID getId() {
        return id;
    }

    public Ration getRation() {
        return ration;
    }

    public UUID getAlimentId() {
        return alimentId;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }
}