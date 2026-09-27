package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrixVenteResponse(
        UUID id,
        BigDecimal prixParLitre,
        LocalDate dateDebut,
        LocalDate dateFin
) {
    public static PrixVenteResponse from(PrixVenteLait prix) {
        return new PrixVenteResponse(
                prix.getId(),
                prix.getPrixParLitre(),
                prix.getDateDebut(),
                prix.getDateFin()
        );
    }
}
