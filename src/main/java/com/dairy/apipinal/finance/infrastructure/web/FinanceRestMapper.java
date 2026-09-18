package com.dairy.apipinal.finance.infrastructure.web;

import com.dairy.apipinal.finance.api.AnimalRentabilityReference;
import com.dairy.apipinal.finance.api.RentabiliteReference;
import org.springframework.stereotype.Component;

@Component
public class FinanceRestMapper {

    public AnimalRentabilityResponse toResponse(
            AnimalRentabilityReference reference
    ) {
        return new AnimalRentabilityResponse(
                reference.animalId(),
                reference.dateDebut(),
                reference.dateFin(),
                reference.volumeLaitKg(),
                reference.volumeLaitLitres(),
                reference.chiffreAffaires(),
                reference.coutAlimentation(),
                reference.marge(),
                reference.prixMoyenParLitre(),
                reference.coutAlimentationParLitre(),
                reference.rentable()
        );
    }

    public RentabiliteResponse toResponse(
            RentabiliteReference reference
    ) {
        return new RentabiliteResponse(
                reference.id(),
                reference.periodeDebut(),
                reference.periodeFin(),
                reference.dateCalcul(),
                reference.volumeLait(),
                reference.chiffreAffaires(),
                reference.coutAlimentation(),
                reference.autresCharges(),
                reference.coutTotal(),
                reference.coutRevientParLitre(),
                reference.marge()
        );
    }
}