package com.dairy.apipinal.reproduction.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "taureau_id")
    private UUID taureauId;

    @Column(name = "code_paillette", length = 100)
    private String codePaillette;

    @Column(name = "date_prevue_velage", nullable = false)
    private LocalDate datePrevueVelage;

    @Column(name = "date_reelle_velage")
    private LocalDate dateReelleVelage;

    @ElementCollection
    @CollectionTable(
            name = "constat_gestation",
            joinColumns = @JoinColumn(name = "cycle_id")
    )
    private List<ConstatGestation> constatsGestation = new ArrayList<>();

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
            UUID taureauId,
            String codePaillette,
            LocalDate datePrevueVelage,
            UUID actorId
    ) {
        this.animalId = animalId;
        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.numeroCycle = numeroCycle;
        this.statut = StatutReproduction.EN_ATTENTE_CONSTAT;
        this.dateInsemination = dateInsemination;
        this.methodeReproduction = methodeReproduction;
        this.taureauId = taureauId;
        this.codePaillette = codePaillette;
        this.datePrevueVelage = datePrevueVelage;

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

        this.constatsGestation.add(new ConstatGestation(date, resultat, veterinaire));

        if ("POSITIF".equalsIgnoreCase(resultat)) {
            this.statut = StatutReproduction.GESTANTE;
        } else if ("NEGATIF".equalsIgnoreCase(resultat)) {
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

    public void interrompre(UUID actorId) {
        if (this.statut == StatutReproduction.EN_ATTENTE_CONSTAT || this.statut == StatutReproduction.GESTANTE) {
            this.statut = StatutReproduction.VIDE;
            this.updatedAt = OffsetDateTime.now();
            this.updatedBy = actorId;
        }
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
    public UUID getTaureauId() { return taureauId; }
    public String getCodePaillette() { return codePaillette; }
    public LocalDate getDatePrevueVelage() { return datePrevueVelage; }
    public LocalDate getDateReelleVelage() { return dateReelleVelage; }
    public List<ConstatGestation> getConstatsGestation() { return new ArrayList<>(constatsGestation); }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
