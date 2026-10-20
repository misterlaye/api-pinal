package com.dairy.apipinal.identity.domain;

import jakarta.persistence.*;

import java.time.Duration;
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

    /**
     * Empreinte BCrypt du code PIN. Le PIN en clair n'est jamais conservé :
     * il n'est communiqué qu'une seule fois, au moment de sa génération.
     * Seuls les membres ouvriers (EMPLOYE, GERANT) disposent d'un PIN.
     */
    @Column(name = "pin_hash", length = 100)
    private String pinHash;

    @Column(name = "pin_failed_attempts", nullable = false)
    private int pinFailedAttempts;

    @Column(name = "pin_locked_until")
    private OffsetDateTime pinLockedUntil;

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
     * Le téléphone est l'identifiant principal (passwordless OTP). Aucun PIN n'est attribué
     * à la création : il est généré uniquement lors de l'ajout comme membre ouvrier.
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

    public String getPinHash() {
        return pinHash;
    }

    public boolean hasPin() {
        return pinHash != null && !pinHash.isBlank();
    }

    public void definirPinHash(String pinHash) {
        if (pinHash == null || pinHash.isBlank()) {
            throw new IllegalArgumentException("L'empreinte du PIN est obligatoire.");
        }
        this.pinHash = pinHash;
        this.pinFailedAttempts = 0;
        this.pinLockedUntil = null;
        this.updatedAt = OffsetDateTime.now();
    }

    public int getPinFailedAttempts() {
        return pinFailedAttempts;
    }

    public OffsetDateTime getPinLockedUntil() {
        return pinLockedUntil;
    }

    public boolean isPinVerrouille(OffsetDateTime now) {
        return pinLockedUntil != null && pinLockedUntil.isAfter(now);
    }

    /**
     * Comptabilise un échec de saisie du PIN et verrouille le compte
     * une fois le nombre maximal d'échecs consécutifs atteint.
     */
    public void enregistrerEchecPin(OffsetDateTime now, int maxEchecs, Duration dureeVerrouillage) {
        if (pinLockedUntil != null && !pinLockedUntil.isAfter(now)) {
            // Le verrouillage précédent est expiré : un nouveau cycle de tentatives commence.
            pinLockedUntil = null;
            pinFailedAttempts = 0;
        }
        pinFailedAttempts++;
        if (pinFailedAttempts >= maxEchecs) {
            pinLockedUntil = now.plus(dureeVerrouillage);
            pinFailedAttempts = 0;
        }
        this.updatedAt = now;
    }

    public void reinitialiserEchecsPin() {
        this.pinFailedAttempts = 0;
        this.pinLockedUntil = null;
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
