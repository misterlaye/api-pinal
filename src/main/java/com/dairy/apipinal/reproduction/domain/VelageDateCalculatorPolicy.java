package com.dairy.apipinal.reproduction.domain;

import java.time.LocalDate;

public interface VelageDateCalculatorPolicy {

    /**
     * Calcule la date prévue de vêlage.
     * @param dateInsemination La date de la saillie ou insémination.
     * @param methode La méthode de reproduction.
     * @return La date prévue de vêlage.
     */
    LocalDate calculate(LocalDate dateInsemination, MethodeReproduction methode);
}
