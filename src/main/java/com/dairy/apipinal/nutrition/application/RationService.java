package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.animal.api.AnimalReference;
import com.dairy.apipinal.nutrition.domain.*;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.PrixAlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RationService {

    private final RationRepository rationRepository;
    private final AlimentRepository alimentRepository;
    private final AnimalQueries animalQueries;
    private final TenantContext tenantContext;
    private final PrixAlimentRepository prixAlimentRepository;

    public RationService(
            RationRepository rationRepository,
            AlimentRepository alimentRepository,
            PrixAlimentRepository prixAlimentRepository,
            AnimalQueries animalQueries,
            TenantContext tenantContext
    ) {
        this.rationRepository = rationRepository;
        this.alimentRepository = alimentRepository;
        this.prixAlimentRepository = prixAlimentRepository;
        this.animalQueries = animalQueries;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public Ration create(CreateRation command) {

        UUID tenantId = tenantContext.currentTenantId();
        UUID exploitationId = tenantContext.currentExploitationId();

        AnimalReference animal = animalQueries.getReference(command.animalId());

        if (!"ACTIF".equals(animal.statut())) {
            throw new IllegalStateException("Une ration ne peut être attribuée qu'à un animal actif.");
        }

        if (!animal.exploitationId().equals(exploitationId)) {
            throw new IllegalArgumentException("L'animal n'appartient pas à cette exploitation.");
        }


        Ration ration = new Ration(
                tenantId,
                exploitationId,
                command.animalId(),
                command.dateDebut(),
                command.origine()
        );

        return rationRepository.save(ration);
    }

    @Transactional
    public Ration addLine(AddRationLine command) {

        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(
                        command.rationId(),
                        tenantId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + command.rationId()
                        )
                );

        if (!ration.getAnimalId().equals(command.animalId())) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        alimentRepository
                .findByIdAndActifTrue(command.alimentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aliment introuvable ou inactif : "
                                        + command.alimentId()
                        )
                );

        ration.ajouterLigne(
                command.alimentId(),
                command.quantite()
        );

        return rationRepository.save(ration);
    }

    @Transactional
    public Ration activate(
            UUID animalId,
            UUID rationId
    ) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(
                        rationId,
                        tenantId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Ration introuvable : " + rationId
                        )
                );

        if (!ration.getAnimalId().equals(animalId)) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        if (ration.getStatut() != StatutRation.BROUILLON) {
            throw new IllegalStateException(
                    "Seule une ration brouillon peut être activée."
            );
        }

        boolean overlapping;

        if (ration.getDateFin() == null) {
            overlapping =
                    rationRepository
                            .existsOverlappingActiveRationWithoutEndDate(
                                    tenantId,
                                    ration.getAnimalId(),
                                    ration.getDateDebut(),
                                    StatutRation.ACTIVE
                            );
        } else {
            overlapping =
                    rationRepository
                            .existsOverlappingActiveRationWithEndDate(
                                    tenantId,
                                    ration.getAnimalId(),
                                    ration.getDateDebut(),
                                    ration.getDateFin(),
                                    StatutRation.ACTIVE
                            );
        }

        if (overlapping) {
            throw new RationAlreadyActiveException();
        }

        ration.activer();

        Ration saved = rationRepository.save(ration);
        saved.getLignes().size();
        return saved;
    }

    @Transactional
    public Ration terminate(TerminateRation command) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(command.rationId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ration introuvable."
                ));

        if (!ration.getAnimalId().equals(command.animalId())) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        ration.terminer(command.dateFin());

        Ration saved = rationRepository.save(ration);
        saved.getLignes().size();
        return saved;
    }

    @Transactional(readOnly = true)
    public Ration getRation(GetRation query) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(query.rationId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ration introuvable."));
        ration.getLignes().size();
        return ration;
    }

    @Transactional(readOnly = true)
    public Page<Ration> getRationsByAnimal(GetRationsByAnimal query) {
        UUID tenantId = tenantContext.currentTenantId();

        animalQueries.getReference(query.animalId());

        Page<Ration> page = rationRepository.findByTenantIdAndAnimalId(
                tenantId,
                query.animalId(),
                query.pageable()
        );
        page.getContent().forEach(r -> r.getLignes().size());
        return page;
    }

    @Transactional(readOnly = true)
    public RationCostResult calculateCost(CalculateRationCost query) {
        UUID tenantId = tenantContext.currentTenantId();

        Ration ration = rationRepository
                .findByIdAndTenantId(query.rationId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ration introuvable."
                ));

        if (!ration.getAnimalId().equals(query.animalId())) {
            throw new IllegalArgumentException(
                    "La ration n'appartient pas à l'animal indiqué."
            );
        }

        BigDecimal total = BigDecimal.ZERO;

        List<RationCostLine> costLines = new ArrayList<>();

        for (LigneRation ligne : ration.getLignes()) {

            PrixAliment prix = prixAlimentRepository
                    .findApplicablePrice(
                            ligne.getAlimentId(),
                            query.dateCalcul()
                    )
                    .orElseThrow(() -> new IllegalStateException(
                            "Aucun prix applicable pour l'aliment "
                                    + ligne.getAlimentId()
                                    + " à la date "
                                    + query.dateCalcul()
                    ));

            BigDecimal cout = ligne.getQuantite()
                    .multiply(prix.getPrixUnitaire());

            costLines.add(
                    new RationCostLine(
                            ligne.getAlimentId(),
                            ligne.getQuantite(),
                            prix.getPrixUnitaire(),
                            cout
                    )
            );

            total = total.add(cout);
        }

        return new RationCostResult(
                ration.getId(),
                query.dateCalcul(),
                costLines,
                total
        );
    }

    /**
     * Coût alimentaire cumulé de l'exploitation sur [dateDebut, dateFinExclusive).
     * Résultat identique à la somme de calculateCost(...) pour chaque ration
     * effective chaque jour, mais calculé en mémoire à partir de deux requêtes
     * (rations + lignes, prix de la période) au lieu de plusieurs par jour et par ration.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCostOverPeriod(
            UUID tenantId,
            UUID exploitationId,
            LocalDate dateDebut,
            LocalDate dateFinExclusive
    ) {
        LocalDate dernierJour = dateFinExclusive.minusDays(1);

        List<Ration> rations = rationRepository.findAllEffectiveRationsOverlappingPeriod(
                tenantId,
                exploitationId,
                dateDebut,
                dernierJour
        );

        Set<UUID> alimentIds = rations.stream()
                .flatMap(ration -> ration.getLignes().stream())
                .map(LigneRation::getAlimentId)
                .collect(Collectors.toSet());

        Map<UUID, List<PrixAliment>> prixParAliment = alimentIds.isEmpty()
                ? Map.of()
                : prixAlimentRepository
                        .findAllOverlappingPeriod(alimentIds, dateDebut, dernierJour)
                        .stream()
                        .collect(Collectors.groupingBy(PrixAliment::getAlimentId));

        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate date = dateDebut; date.isBefore(dateFinExclusive); date = date.plusDays(1)) {
            for (Ration ration : rations) {
                if (!estEffective(ration, date)) {
                    continue;
                }
                for (LigneRation ligne : ration.getLignes()) {
                    PrixAliment prix = prixApplicable(
                            prixParAliment.getOrDefault(ligne.getAlimentId(), List.of()),
                            ligne.getAlimentId(),
                            date
                    );
                    total = total.add(ligne.getQuantite().multiply(prix.getPrixUnitaire()));
                }
            }
        }

        return total;
    }

    private static boolean estEffective(Ration ration, LocalDate date) {
        return !ration.getDateDebut().isAfter(date)
                && (ration.getDateFin() == null || !ration.getDateFin().isBefore(date));
    }

    private static PrixAliment prixApplicable(
            List<PrixAliment> prix,
            UUID alimentId,
            LocalDate date
    ) {
        List<PrixAliment> applicables = prix.stream()
                .filter(p -> !p.getDateDebut().isAfter(date)
                        && (p.getDateFin() == null || !p.getDateFin().isBefore(date)))
                .toList();

        if (applicables.size() != 1) {
            throw new IllegalStateException(
                    (applicables.isEmpty() ? "Aucun prix applicable" : "Plusieurs prix applicables")
                            + " pour l'aliment " + alimentId
                            + " à la date " + date
            );
        }
        return applicables.getFirst();
    }
}