package com.dairy.apipinal.reproduction.domain;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class ConstatGestation {

    private LocalDate constatDate;
    private String constatResultat;
    private String constatVeterinaire;

    protected ConstatGestation() {
    }

    public ConstatGestation(LocalDate date, String resultat, String veterinaire) {
        this.constatDate = date;
        this.constatResultat = resultat;
        this.constatVeterinaire = veterinaire;
    }

    public LocalDate getDate() {
        return constatDate;
    }

    public String getResultat() {
        return constatResultat;
    }

    public String getVeterinaire() {
        return constatVeterinaire;
    }
}
