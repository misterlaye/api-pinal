package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.api.AnimalRentabilityReference;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.RentabiliteReference;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FinanceQueriesImpl implements FinanceQueries {

    private final AnimalRentabilityService animalRentabilityService;
    private final CalculRentabiliteRepository calculRentabiliteRepository;
    private final TenantContext tenantContext;

    public FinanceQueriesImpl(
            AnimalRentabilityService animalRentabilityService,
            CalculRentabiliteRepository calculRentabiliteRepository,
            TenantContext tenantContext
    ) {
        this.animalRentabilityService = animalRentabilityService;
        this.calculRentabiliteRepository = calculRentabiliteRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public Optional<AnimalRentabilityReference> calculateAnimalRentability(
            UUID animalId,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        return animalRentabilityService
                .calculate(animalId, dateDebut, dateFin)
                .map(this::toAnimalReference);
    }

    @Override
    public Optional<RentabiliteReference> findLatestRentabilite() {

        return calculRentabiliteRepository
                .findFirstByTenantIdOrderByDateCalculDesc(
                        tenantContext.currentTenantId()
                )
                .map(this::toRentabiliteReference);
    }
    private AnimalRentabilityReference toAnimalReference(
            AnimalRentabilityResult result
    ) {
        return new AnimalRentabilityReference(
                result.animalId(),
                result.dateDebut(),
                result.dateFin(),
                result.volumeLaitKg(),
                result.volumeLaitLitres(),
                result.chiffreAffaires(),
                result.coutAlimentation(),
                result.marge(),
                result.prixMoyenParLitre(),
                result.coutAlimentationParLitre(),
                result.rentable()
        );
    }

    private RentabiliteReference toRentabiliteReference(
            CalculRentabilite calcul
    ) {
        return new RentabiliteReference(
                calcul.getId(),
                calcul.getPeriodeDebut(),
                calcul.getPeriodeFin(),
                calcul.getDateCalcul(),
                calcul.getVolumeLait(),
                calcul.getChiffreAffaires(),
                calcul.getCoutAlimentation(),
                calcul.getAutresCharges(),
                calcul.getCoutTotal(),
                calcul.getCoutRevientParLitre(),
                calcul.getMarge()
        );
    }
}