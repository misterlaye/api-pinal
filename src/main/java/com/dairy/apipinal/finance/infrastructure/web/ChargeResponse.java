package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.domain.ChargeExploitation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ChargeResponse(
        UUID id,
        String libelle,
        String categorie,
        BigDecimal montant,
        LocalDate date,
        String description
) {
    public static ChargeResponse from(ChargeExploitation charge) {
        return new ChargeResponse(
                charge.getId(),
                charge.getLibelle(),
                charge.getCategorie(),
                charge.getMontant(),
                charge.getDate(),
                charge.getDescription()
        );
    }
}
