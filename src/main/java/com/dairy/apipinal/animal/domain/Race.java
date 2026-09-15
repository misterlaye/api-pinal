package com.dairy.apipinal.animal.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "race",
        uniqueConstraints = {@UniqueConstraint(name = "uk_race_code", columnNames = "code")}
)
public class Race {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false)
    private boolean actif;

    protected Race() {
    }

    public Race(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
        this.actif = true;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isActif() {
        return actif;
    }

    public void desactiver() {
        this.actif = false;
    }

    public void activer() {
        this.actif = true;
    }
}
