package com.dairy.apipinal.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "prix_aliment")
public class PrixAliment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aliment_id", nullable = false)
    private UUID alimentId;

    @Column(name = "prix_unitaire", nullable = false, precision = 19, scale = 4)
    private BigDecimal prixUnitaire;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Version
    private long version;

    protected PrixAliment() {
    }

    public PrixAliment(
            UUID alimentId,
            BigDecimal prixUnitaire,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        this.alimentId = Objects.requireNonNull(
                alimentId,
                "L'aliment est obligatoire."
        );

        this.prixUnitaire = requirePositive(
                prixUnitaire,
                "Le prix unitaire doit être strictement positif."
        );

        this.dateDebut = Objects.requireNonNull(
                dateDebut,
                "La date de début est obligatoire."
        );

        validatePeriod(dateDebut, dateFin);

        this.dateFin = dateFin;
    }

    private static BigDecimal requirePositive(
            BigDecimal value,
            String message
    ) {
        Objects.requireNonNull(value, message);

        if (value.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static void validatePeriod(
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getAlimentId() {
        return alimentId;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public long getVersion() {
        return version;
    }
}