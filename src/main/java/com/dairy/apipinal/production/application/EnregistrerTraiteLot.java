package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Saisie de traite par lot (une séance, plusieurs animaux).
 * <p>
 * Chaque ligne est vérifiée avant écriture avec les mêmes règles que la saisie individuelle
 * ({@link RecordMilking#motifRefus}). Les lignes valides sont enregistrées ; les autres sont
 * renvoyées dans {@link Resultat#rejets()} avec leur motif — aucune ligne n'est ignorée en silence.
 */
@Service
public class EnregistrerTraiteLot {

    private final LactationRepository lactationRepository;
    private final RecordMilking recordMilking;

    public EnregistrerTraiteLot(
            LactationRepository lactationRepository,
            RecordMilking recordMilking
    ) {
        this.lactationRepository = lactationRepository;
        this.recordMilking = recordMilking;
    }

    @Transactional
    public Resultat execute(Command command) {
        List<Traite> enregistrees = new ArrayList<>();
        List<Rejet> rejets = new ArrayList<>();
        Set<UUID> animauxVus = new HashSet<>();
        OffsetDateTime dateHeure = command.dateHeure() != null ? command.dateHeure() : OffsetDateTime.now();

        for (SaisieItem item : command.items()) {
            if (item.animalId() == null) {
                rejets.add(new Rejet(null, "Animal non précisé."));
                continue;
            }
            if (!animauxVus.add(item.animalId())) {
                rejets.add(new Rejet(item.animalId(), "Animal saisi plusieurs fois dans le lot."));
                continue;
            }
            if (item.quantiteLitres() == null || item.quantiteLitres().signum() <= 0) {
                rejets.add(new Rejet(item.animalId(), "Quantité absente ou nulle."));
                continue;
            }

            Optional<Lactation> lactation = lactationRepository.findByAnimalIdAndTenantIdAndExploitationIdAndStatut(
                    item.animalId(), command.tenantId(), command.exploitationId(), StatutLactation.EN_COURS);
            if (lactation.isEmpty()) {
                rejets.add(new Rejet(item.animalId(), "Aucune lactation en cours pour cet animal."));
                continue;
            }

            RecordMilking.Command traite = new RecordMilking.Command(
                    command.tenantId(),
                    command.exploitationId(),
                    lactation.get().getId(),
                    command.auteurId(),
                    dateHeure,
                    command.type(),
                    item.quantiteLitres()
            );
            Optional<String> motif = recordMilking.motifRefus(lactation.get(), traite);
            if (motif.isPresent()) {
                rejets.add(new Rejet(item.animalId(), motif.get()));
                continue;
            }
            enregistrees.add(recordMilking.execute(traite));
        }

        return new Resultat(enregistrees, rejets);
    }

    public record SaisieItem(
            UUID animalId,
            BigDecimal quantiteLitres
    ) {}

    public record Rejet(UUID animalId, String motif) {}

    public record Resultat(List<Traite> enregistrees, List<Rejet> rejets) {}

    public record Command(
            UUID tenantId,
            UUID exploitationId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            List<SaisieItem> items
    ) {
    }
}
