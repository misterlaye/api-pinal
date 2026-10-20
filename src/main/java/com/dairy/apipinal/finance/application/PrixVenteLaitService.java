package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Historique des prix de vente du lait de l'exploitation courante.
 * <p>
 * Règle de succession : un nouveau prix commençant après le début du prix en cours (sans date de fin)
 * clôt ce dernier la veille de la nouvelle date de début. Tout autre recouvrement de période est refusé.
 */
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
        UUID exploitationId = tenantContext.currentExploitationId();

        validatePeriod(
                command.dateDebut(),
                command.dateFin()
        );

        repository.findFirstByTenantIdAndExploitationIdAndDateFinIsNull(tenantId, exploitationId)
                .filter(prixEnCours -> prixEnCours.getDateDebut().isBefore(command.dateDebut()))
                .ifPresent(prixEnCours -> {
                    prixEnCours.cloturer(command.dateDebut().minusDays(1));
                    // Écrit immédiatement : la contrainte d'exclusion SQL est vérifiée à l'insertion du nouveau prix.
                    repository.saveAndFlush(prixEnCours);
                });

        boolean overlapping = command.dateFin() == null
                ? repository.existsOverlappingOpenEndedPeriod(tenantId, exploitationId, command.dateDebut())
                : repository.existsOverlappingPeriodWithEndDate(tenantId, exploitationId, command.dateDebut(), command.dateFin());

        if (overlapping) {
            throw new IllegalStateException(
                    "La période du prix de vente du lait chevauche une période existante."
            );
        }

        PrixVenteLait price = new PrixVenteLait(
                tenantId,
                exploitationId,
                command.prixParLitre(),
                command.dateDebut(),
                command.dateFin()
        );

        return repository.save(price);
    }

    @Transactional(readOnly = true)
    public PrixVenteLait getApplicablePrice(LocalDate date) {

        return repository
                .findApplicablePrice(
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId(),
                        date
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Aucun prix de vente du lait applicable à la date : "
                                        + date
                        )
                );
    }

    @Transactional(readOnly = true)
    public List<PrixVenteLait> getHistory() {

        return repository.findAllByTenantIdAndExploitationIdOrderByDateDebutDesc(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId()
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
