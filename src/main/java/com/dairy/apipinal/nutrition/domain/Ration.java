package com.dairy.apipinal.nutrition.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ration")
public class Ration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "animal_id", nullable = false)
    private UUID animalId;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutRation statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigineRation origine;

    @OneToMany(
            mappedBy = "ration",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private final List<LigneRation> lignes = new ArrayList<>();

    @Version
    private long version;

    protected Ration() {
    }

    public Ration(
            UUID animalId,
            LocalDate dateDebut,
            OrigineRation origine
    ) {
        this.animalId = Objects.requireNonNull(
                animalId,
                "L'animal est obligatoire."
        );

        this.dateDebut = Objects.requireNonNull(
                dateDebut,
                "La date de début est obligatoire."
        );

        this.origine = Objects.requireNonNull(
                origine,
                "L'origine est obligatoire."
        );

        this.statut = StatutRation.BROUILLON;
    }

    public void ajouterLigne(
            UUID alimentId,
            BigDecimal quantite
    ) {
        Objects.requireNonNull(alimentId, "L'aliment est obligatoire.");

        boolean alreadyPresent = lignes.stream()
                .anyMatch(ligne -> ligne.getAlimentId().equals(alimentId));

        if (alreadyPresent) {
            throw new IllegalArgumentException("Un aliment ne peut apparaître qu'une seule fois dans une ration.");
        }

        lignes.add(new LigneRation(this, alimentId, quantite));
    }

    public void activer() {
        if (lignes.isEmpty()) {
            throw new IllegalStateException(
                    "Une ration doit contenir au moins une ligne avant activation."
            );
        }

        if (statut == StatutRation.TERMINEE) {
            throw new IllegalStateException(
                    "Une ration terminée ne peut pas être réactivée."
            );
        }

        this.statut = StatutRation.ACTIVE;
    }

    public void terminer(LocalDate dateFin) {
        Objects.requireNonNull(
                dateFin,
                "La date de fin est obligatoire."
        );

        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure au début."
            );
        }

        if (statut != StatutRation.ACTIVE) {
            throw new IllegalStateException(
                    "Seule une ration active peut être terminée."
            );
        }

        this.dateFin = dateFin;
        this.statut = StatutRation.TERMINEE;
    }

    public UUID getId() {
        return id;
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

    public StatutRation getStatut() {
        return statut;
    }

    public OrigineRation getOrigine() {
        return origine;
    }

    public List<LigneRation> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    public long getVersion() {
        return version;
    }
}