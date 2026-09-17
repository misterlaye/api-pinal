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
}