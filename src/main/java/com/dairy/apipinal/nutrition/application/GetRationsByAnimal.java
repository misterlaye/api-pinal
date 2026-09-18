package com.dairy.apipinal.nutrition.application;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetRationsByAnimal(
        UUID animalId,
        Pageable pageable
) {}