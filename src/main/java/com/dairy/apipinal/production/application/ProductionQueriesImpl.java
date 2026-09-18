package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.production.api.ProductionReference;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.production.infrastructure.persistence.TraiteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductionQueriesImpl implements ProductionQueries {

    private final LactationRepository lactationRepository;
    private final TraiteRepository traiteRepository;
    private final TenantContext tenantContext;

    public ProductionQueriesImpl(
            LactationRepository lactationRepository,
            TraiteRepository traiteRepository,
            TenantContext tenantContext
    ) {
        this.lactationRepository = lactationRepository;
        this.traiteRepository = traiteRepository;
        this.tenantContext = tenantContext;
    }

    @Override
    public ProductionReference getActiveLactation(UUID animalId) {

        return lactationRepository
                .findByAnimalIdAndTenantIdAndStatut(
                        animalId,
                        tenantContext.currentTenantId(),
                        StatutLactation.EN_COURS
                )
                .map(lactation -> new ProductionReference(
                        lactation.getId(),
                        lactation.getAnimalId(),
                        lactation.getDateDebut(),
                        lactation.getDateFin(),
                        lactation.getStatut()
                ))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune lactation active pour cet animal."
                ));
    }

    @Override
    public BigDecimal getMilkProductionKg(
            UUID animalId,
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    ) {
        validatePeriod(dateDebut, dateFinExclusive);

        UUID tenantId = tenantContext.currentTenantId();

        List<Lactation> lactations =
                lactationRepository.findAllOverlappingPeriod(
                        animalId,
                        tenantId,
                        dateDebut.toLocalDate(),
                        dateFinExclusive.minusNanos(1).toLocalDate()
                );

        return lactations.stream()
                .map(lactation ->
                        traiteRepository.sumQuantiteKgByLactationAndPeriod(
                                lactation.getId(),
                                tenantId,
                                dateDebut,
                                dateFinExclusive
                        )
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalMilkProductionKg(
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    ) {
        validatePeriod(dateDebut, dateFinExclusive);

        return traiteRepository.sumQuantiteKgByTenantAndPeriod(
                tenantContext.currentTenantId(),
                dateDebut,
                dateFinExclusive
        );
    }

    private void validatePeriod(
            OffsetDateTime dateDebut,
            OffsetDateTime dateFinExclusive
    ) {
        if (dateDebut == null || dateFinExclusive == null) {
            throw new IllegalArgumentException(
                    "Les bornes de la période sont obligatoires."
            );
        }

        if (!dateDebut.isBefore(dateFinExclusive)) {
            throw new IllegalArgumentException(
                    "La date de début doit être antérieure à la date de fin."
            );
        }
    }
}