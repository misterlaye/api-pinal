package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.PrixAlimentRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Historique des prix d'un aliment du catalogue de l'exploitation courante.
 * <p>
 * Règle de succession : un nouveau prix commençant après le début du prix en cours (sans date de fin)
 * clôt ce dernier la veille de la nouvelle date de début. Tout autre recouvrement de période est refusé.
 */
@Service
public class PrixAlimentService {

    private final AlimentRepository alimentRepository;
    private final PrixAlimentRepository prixAlimentRepository;
    private final TenantContext tenantContext;

    public PrixAlimentService(
            AlimentRepository alimentRepository,
            PrixAlimentRepository prixAlimentRepository,
            TenantContext tenantContext
    ) {
        this.alimentRepository = alimentRepository;
        this.prixAlimentRepository = prixAlimentRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public PrixAliment create(CreatePrixAliment command) {

        Aliment aliment = requireAliment(command.alimentId());

        if (command.dateDebut() == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }
        if (command.dateFin() != null
                && command.dateFin().isBefore(command.dateDebut())) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }

        prixAlimentRepository.findFirstByAlimentIdAndDateFinIsNull(aliment.getId())
                .filter(prixEnCours -> prixEnCours.getDateDebut().isBefore(command.dateDebut()))
                .ifPresent(prixEnCours -> {
                    prixEnCours.cloturerAu(command.dateDebut().minusDays(1));
                    // Écrit immédiatement : la contrainte d'exclusion SQL est vérifiée à l'insertion du nouveau prix.
                    prixAlimentRepository.saveAndFlush(prixEnCours);
                });

        boolean overlapping = command.dateFin() != null
                ? prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                        aliment.getId(), command.dateDebut(), command.dateFin())
                : prixAlimentRepository.existsOverlappingOpenEndedPeriod(
                        aliment.getId(), command.dateDebut());

        if (overlapping) {
            throw new IllegalStateException(
                    "La période du prix chevauche une période existante."
            );
        }

        PrixAliment prixAliment = new PrixAliment(
                aliment.getId(),
                command.prixUnitaire(),
                command.dateDebut(),
                command.dateFin()
        );

        return prixAlimentRepository.save(prixAliment);
    }

    @Transactional(readOnly = true)
    public List<PrixAliment> getHistory(GetPrixAlimentHistory query) {
        Aliment aliment = requireAliment(query.alimentId());
        return prixAlimentRepository.findAllByAlimentIdOrderByDateDebutDesc(aliment.getId());
    }

    @Transactional(readOnly = true)
    public Optional<PrixAliment> getApplicablePrice(GetApplicablePrixAliment query) {
        Aliment aliment = requireAliment(query.alimentId());
        return prixAlimentRepository.findApplicablePrice(aliment.getId(), query.date());
    }

    private Aliment requireAliment(UUID alimentId) {
        return alimentRepository
                .findByIdAndTenantIdAndExploitationId(
                        alimentId,
                        tenantContext.currentTenantId(),
                        tenantContext.currentExploitationId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Aliment introuvable."));
    }
}
