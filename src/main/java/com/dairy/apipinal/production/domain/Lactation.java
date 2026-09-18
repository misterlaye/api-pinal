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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lactation")
public class Lactation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id")
    private UUID exploitationId;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutLactation statut;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    protected Lactation() {
    }

    public Lactation(
            UUID tenantId,
            UUID animalId,
            LocalDate dateDebut,
            UUID actorId
    ) {
        if (dateDebut == null) {
            throw new IllegalArgumentException(
                    "La date de début est obligatoire."
            );
        }

        this.tenantId = tenantId;
        this.animalId = animalId;
        this.dateDebut = dateDebut;
        this.statut = StatutLactation.EN_COURS;

        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void terminer(LocalDate dateFin, UUID actorId) {

        if (statut == StatutLactation.TERMINEE) {
            throw new IllegalStateException(
                    "La lactation est déjà terminée."
            );
        }

        if (dateFin == null || dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La date de fin doit être supérieure ou égale à la date de début."
            );
        }

        this.dateFin = dateFin;
        this.statut = StatutLactation.TERMINEE;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
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

    public UUID getAnimalId() {
        return animalId;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public StatutLactation getStatut() {
        return statut;
    }
}