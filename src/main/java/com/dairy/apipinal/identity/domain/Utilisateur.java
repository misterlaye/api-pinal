package com.dairy.apipinal.identity.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    private UUID id;

    @Column(length = 20)
    private String telephone;

    @Column(length = 320)
    private String email;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(nullable = false, length = 120)
    private String prenom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutUtilisateur statut;

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

    protected Utilisateur() {
    }

    /**
     * Crée un utilisateur.
     * Le téléphone est l'identifiant principal (passwordless OTP).
     */
    public Utilisateur(
            UUID userId,
            String telephone,
            String nom,
            String prenom
    ) {
        this.id = userId;
        this.telephone = telephone;
        this.nom = nom;
        this.prenom = prenom;
        this.statut = StatutUtilisateur.ACTIF;

        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getEmail() {
        return email;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public StatutUtilisateur getStatut() {
        return statut;
    }

    public void updateProfil(String nom, String prenom, String email) {
        if (nom != null && !nom.isBlank()) {
            this.nom = nom;
        }
        if (prenom != null && !prenom.isBlank()) {
            this.prenom = prenom;
        }
        if (email != null) {
            this.email = email;
        }
        this.updatedAt = OffsetDateTime.now();
    }
}
