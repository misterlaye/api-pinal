package com.dairy.apipinal.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "traite")
public class Traite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id")
    private UUID exploitationId;

    @Column(name = "lactation_id", nullable = false)
    private UUID lactationId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "date_heure", nullable = false)
    private OffsetDateTime dateHeure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeTraite type;

    @Column(name = "quantite_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantiteKg;

    @Column(name = "date_heure_saisie", nullable = false)
    private OffsetDateTime dateHeureSaisie;

    @Version
    @Column(nullable = false)
    private long version;

    protected Traite() {
    }

    public Traite(
            UUID tenantId,
            UUID lactationId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            BigDecimal quantiteKg,
            OffsetDateTime dateHeureSaisie
    ) {

        if (quantiteKg == null || quantiteKg.signum() < 0) {
            throw new IllegalArgumentException(
                    "La quantité de lait doit être supérieure ou égale à zéro."
            );
        }

        if (dateHeure == null) {
            throw new IllegalArgumentException(
                    "La date et l'heure de traite sont obligatoires."
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Le type de traite est obligatoire."
            );
        }

        if (auteurId == null) {
            throw new IllegalArgumentException(
                    "L'auteur de la saisie est obligatoire."
            );
        }

        this.tenantId = tenantId;
        this.lactationId = lactationId;
        this.auteurId = auteurId;
        this.dateHeure = dateHeure;
        this.type = type;
        this.quantiteKg = quantiteKg;
        this.dateHeureSaisie = dateHeureSaisie;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getExploitationId() {
        return exploitationId;
    }

    public UUID getLactationId() {
        return lactationId;
    }

    public UUID getAuteurId() {
        return auteurId;
    }

    public OffsetDateTime getDateHeure() {
        return dateHeure;
    }

    public TypeTraite getType() {
        return type;
    }

    public BigDecimal getQuantiteKg() {
        return quantiteKg;
    }

    public OffsetDateTime getDateHeureSaisie() {
        return dateHeureSaisie;
    }
}