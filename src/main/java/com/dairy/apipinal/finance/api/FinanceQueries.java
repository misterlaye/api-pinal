package com.dairy.apipinal.finance.api;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface FinanceQueries {

    Optional<AnimalRentabilityReference> calculateAnimalRentability(
            UUID animalId,
            LocalDate dateDebut,
            LocalDate dateFin
    );

    Optional<RentabiliteReference> findLatestRentabilite();

    /**
     * Résultat financier de l'exploitation courante sur [debut, fin] (bornes incluses), selon les règles
     * de {@link ResultatFinancier}. Source unique des indicateurs financiers.
     */
    ResultatFinancier calculerPeriode(LocalDate debut, LocalDate fin);

    com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse getDashboardSummary();
}