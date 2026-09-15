package com.dairy.apipinal.health.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evenement_sanitaire")
public class EvenementSanitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "date_heure", nullable = false)
    private OffsetDateTime dateHeure;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String diagnostic;

    @Column(length = 2000)
    private String traitement;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected EvenementSanitaire() {
    }

    public EvenementSanitaire(
            UUID tenantId,
            UUID animalId,
            OffsetDateTime dateHeure,
            String description,
            UUID actorId
    ) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Le tenant est obligatoire.");
        }

        if (animalId == null) {
            throw new IllegalArgumentException("L'animal est obligatoire.");
        }

        if (dateHeure == null) {
            throw new IllegalArgumentException(
                    "La date et l'heure sont obligatoires."
            );
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "La description est obligatoire."
            );
        }

        if (actorId == null) {
            throw new IllegalArgumentException(
                    "L'auteur est obligatoire."
            );
        }

        this.tenantId = tenantId;
        this.animalId = animalId;
        this.dateHeure = dateHeure;
        this.description = description;
        this.createdBy = actorId;
        this.updatedBy = actorId;

        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            String description,
            String diagnostic,
            String traitement,
            LocalDate dateFin,
            UUID actorId
    ) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "La description est obligatoire."
            );
        }

        if (dateFin != null &&
                dateFin.isBefore(dateHeure.toLocalDate())) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de l'événement."
            );
        }

        this.description = description;
        this.diagnostic = diagnostic;
        this.traitement = traitement;
        this.dateFin = dateFin;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAnimalId() {
        return animalId;
    }

    public OffsetDateTime getDateHeure() {
        return dateHeure;
    }

    public String getDescription() {
        return description;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public String getTraitement() {
        return traitement;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }
}