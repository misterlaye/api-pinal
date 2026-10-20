package com.dairy.apipinal.nutrition.api;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Aliment distribué (ration effective) sans prix applicable sur une partie de la période.
 */
public record PrixAlimentManquant(
        UUID alimentId,
        String alimentNom,
        LocalDate premierJour,
        LocalDate dernierJour
) {
}
