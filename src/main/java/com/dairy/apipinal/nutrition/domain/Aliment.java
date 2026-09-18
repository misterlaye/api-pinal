package com.dairy.apipinal.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "aliment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_aliment_code",
                        columnNames = "code"
                )
        }
)
public class Aliment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 100)
    private String categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UniteAliment unite;

    @Column(nullable = false)
    private boolean actif;

    @Column(nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(nullable = false)
    private Instant dateModification;

    @Version
    private long version;

    protected Aliment() {
    }

    public Aliment(
            String code,
            String nom,
            String categorie,
            UniteAliment unite
    ) {
        this.code = requireText(code, "Le code est obligatoire.");
        this.nom = requireText(nom, "Le nom est obligatoire.");
        this.categorie = requireText(categorie, "La catégorie est obligatoire.");
        this.unite = Objects.requireNonNull(
                unite,
                "L'unité est obligatoire."
        );

        this.actif = true;
        this.dateCreation = Instant.now();
        this.dateModification = this.dateCreation;
    }

    public void update(
            String nom,
            String categorie,
            UniteAliment unite
    ) {
        this.nom = requireText(nom, "Le nom est obligatoire.");
        this.categorie = requireText(categorie, "La catégorie est obligatoire.");
        this.unite = Objects.requireNonNull(
                unite,
                "L'unité est obligatoire."
        );

        this.dateModification = Instant.now();
    }

    public void desactiver() {
        this.actif = false;
        this.dateModification = Instant.now();
    }

    public void activer() {
        this.actif = true;
        this.dateModification = Instant.now();
    }

    private static String requireText(String value, String message) {
        Objects.requireNonNull(value, message);

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    public String getCategorie() {
        return categorie;
    }

    public UniteAliment getUnite() {
        return unite;
    }

    public boolean isActif() {
        return actif;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public Instant getDateModification() {
        return dateModification;
    }

    public long getVersion() {
        return version;
    }
}