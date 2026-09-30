package com.dairy.apipinal.reproduction.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DefaultVelageDateCalculator implements VelageDateCalculatorPolicy {

    @Override
    public LocalDate calculate(LocalDate dateInsemination, MethodeReproduction methode) {
        // En V1, prédiction standard à +283 jours pour une vache laitière
        return dateInsemination.plusDays(283);
    }
}
