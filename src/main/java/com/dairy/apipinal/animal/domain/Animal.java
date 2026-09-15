package com.dairy.apipinal.animal.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "animal",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_animal_exploitation_identifiant", columnNames = {"exploitation_id", "identifiant"})
        }
)
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id", nullable = false)
    private UUID exploitationId;

    @Column(name = "race_id", nullable = false)
    private UUID raceId;

    @Column(nullable = false, length = 100)
    private String identifiant;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAnimal statut;

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

    protected Animal() {
    }

    public Animal(
            UUID tenantId,
            UUID exploitationId,
            UUID raceId,
            String identifiant,
            String nom,
            String photoUrl,
            LocalDate dateNaissance,
            UUID actorId
    ) {
        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.raceId = raceId;
        this.identifiant = identifiant;
        this.nom = nom;
        this.photoUrl = photoUrl;
        this.dateNaissance = dateNaissance;
        this.statut = StatutAnimal.ACTIF;

        OffsetDateTime now = OffsetDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void update(
            UUID raceId,
            String identifiant,
            String nom,
            String photoUrl,
            LocalDate dateNaissance,
            UUID actorId
    ) {
        ensureEditable();

        this.raceId = raceId;
        this.identifiant = identifiant;
        this.nom = nom;
        this.photoUrl = photoUrl;
        this.dateNaissance = dateNaissance;

        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public void changeStatus(StatutAnimal newStatus, UUID actorId) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Le statut de l'animal est obligatoire.");
        }

        if (this.statut == newStatus) {
            return;
        }

        this.statut = newStatus;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    private void ensureEditable() {
        if (this.statut != StatutAnimal.ACTIF) {
            throw new IllegalStateException(
                    "Un animal vendu ou décédé ne peut plus être modifié."
            );
        }
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

    public UUID getRaceId() {
        return raceId;
    }

    public String getIdentifiant() {
        return identifiant;
    }

    public String getNom() {
        return nom;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }

    public StatutAnimal getStatut() {
        return statut;
    }
}
