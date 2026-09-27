package com.dairy.apipinal.reproduction.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "cycle_reproduction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cycle_repro_numero", columnNames = {"animal_id", "numero_cycle"})
        }
)
public class CycleReproduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id", nullable = false)
    private UUID exploitationId;

    @Column(name = "numero_cycle", nullable = false)
    private int numeroCycle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutReproduction statut;

    @Column(name = "date_insemination", nullable = false)
    private LocalDate dateInsemination;

    @Enumerated(EnumType.STRING)
    @Column(name = "methode_reproduction", nullable = false, length = 50)
    private MethodeReproduction methodeReproduction;

    @Column(name = "identifiant_taureau", length = 100)
    private String identifiantTaureau;

    @Column(name = "date_prevue_velage", nullable = false)
    private LocalDate datePrevueVelage;

    @Column(name = "date_reelle_velage")
    private LocalDate dateReelleVelage;

    @Embedded
    private ConstatGestation constatGestation;

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

    protected CycleReproduction() {
    }

    public CycleReproduction(
            UUID animalId,
            UUID tenantId,
            UUID exploitationId,
            int numeroCycle,
            LocalDate dateInsemination,
            MethodeReproduction methodeReproduction,
            String identifiantTaureau,
            UUID actorId
    ) {
        this.animalId = animalId;
        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.numeroCycle = numeroCycle;
        this.statut = StatutReproduction.EN_ATTENTE_CONSTAT;
        this.dateInsemination = dateInsemination;
        this.methodeReproduction = methodeReproduction;
        this.identifiantTaureau = identifiantTaureau;

        // Approximativement 283 jours de gestation pour une vache laitière
        this.datePrevueVelage = dateInsemination.plusDays(283);

        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void enregistrerConstat(LocalDate date, String resultat, String veterinaire, UUID actorId) {
        if (this.statut != StatutReproduction.EN_ATTENTE_CONSTAT) {
            throw new IllegalStateException("Le constat de gestation n'est possible qu'en statut EN_ATTENTE_CONSTAT.");
        }

        this.constatGestation = new ConstatGestation(date, resultat, veterinaire);

        if ("POSITIF".equalsIgnoreCase(resultat)) {
            this.statut = StatutReproduction.GESTANTE;
        } else {
            this.statut = StatutReproduction.VIDE;
        }

        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public void declarerVelage(LocalDate dateReelle, UUID actorId) {
        if (this.statut != StatutReproduction.GESTANTE) {
            throw new IllegalStateException("Le vêlage ne peut être déclaré que pour une vache gestante.");
        }

        this.dateReelleVelage = dateReelle;
        this.statut = StatutReproduction.TERMINEE_VELAGE;

        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    public void declarerAvortement(UUID actorId) {
        if (this.statut != StatutReproduction.GESTANTE) {
            throw new IllegalStateException("L'avortement ne peut être déclaré que pour une vache gestante.");
        }

        this.statut = StatutReproduction.AVORTEE;
        this.updatedAt = OffsetDateTime.now();
        this.updatedBy = actorId;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getAnimalId() { return animalId; }
    public UUID getTenantId() { return tenantId; }
    public UUID getExploitationId() { return exploitationId; }
    public int getNumeroCycle() { return numeroCycle; }
    public StatutReproduction getStatut() { return statut; }
    public LocalDate getDateInsemination() { return dateInsemination; }
    public MethodeReproduction getMethodeReproduction() { return methodeReproduction; }
    public String getIdentifiantTaureau() { return identifiantTaureau; }
    public LocalDate getDatePrevueVelage() { return datePrevueVelage; }
    public LocalDate getDateReelleVelage() { return dateReelleVelage; }
    public ConstatGestation getConstatGestation() { return constatGestation; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
