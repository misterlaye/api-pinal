package com.dairy.apipinal.animal.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MouvementSortieRequest(
        LocalDate dateSortie,
        String motif,
        BigDecimal prixVente
) {
}
