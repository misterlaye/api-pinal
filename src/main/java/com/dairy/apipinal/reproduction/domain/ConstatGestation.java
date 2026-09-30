package com.dairy.apipinal.reproduction.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class ConstatGestation {

    @Column(name = "date_constat", nullable = false)
    private LocalDate date;

    @Column(name = "resultat", nullable = false, length = 20)
    private String resultat;

    @Column(name = "veterinaire", length = 100)
    private String veterinaire;

    protected ConstatGestation() {
    }

    public ConstatGestation(LocalDate date, String resultat, String veterinaire) {
        this.date = date;
        this.resultat = resultat;
        this.veterinaire = veterinaire;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getResultat() {
        return resultat;
    }

    public String getVeterinaire() {
        return veterinaire;
    }
}
