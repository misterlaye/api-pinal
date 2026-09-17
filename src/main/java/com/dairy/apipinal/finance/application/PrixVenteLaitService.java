package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.application.CreatePrixVenteLait;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PrixVenteLaitService {

    private final PrixVenteLaitRepository repository;
    private final TenantContext tenantContext;

    public PrixVenteLaitService(
            PrixVenteLaitRepository repository,
            TenantContext tenantContext
    ) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    public PrixVenteLait create(CreatePrixVenteLait command) {

        UUID tenantId = tenantContext.currentTenantId();

        validatePeriod(
                command.dateDebut(),
                command.dateFin()
        );

        boolean overlapping;

        if (command.dateFin() == null) {
            overlapping =
                    repository.existsOverlappingOpenEndedPeriod(
                            tenantId,
                            command.dateDebut()
                    );
        } else {
            overlapping =
                    repository.existsOverlappingPeriodWithEndDate(
                            tenantId,
                            command.dateDebut(),
                            command.dateFin()
                    );
        }

        if (overlapping) {
            throw new IllegalStateException(
                    "La période du prix de vente du lait chevauche une période existante."
            );
        }

        PrixVenteLait price = new PrixVenteLait(
                tenantId,
                command.prixParLitre(),
                command.dateDebut(),
                command.dateFin()
        );

        return repository.save(price);
    }

    @Transactional(readOnly = true)
    public PrixVenteLait getApplicablePrice(LocalDate date) {

        UUID tenantId = tenantContext.currentTenantId();

        return repository
                .findApplicablePrice(tenantId, date)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Aucun prix de vente du lait applicable à la date : "
                                        + date
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<PrixVenteLait> getHistory() {

        UUID tenantId = tenantContext.currentTenantId();

        return repository.findAllByTenantIdOrderByDateDebutDesc(
                tenantId
        );
    }

    private void validatePeriod(
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        if (dateDebut == null) {
            throw new IllegalArgumentException(
                    "La date de début est obligatoire."
            );
        }

        if (dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }
    }
}