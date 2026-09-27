package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public List<Traite> execute(Command command) {
        List<Traite> traites = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (SaisieItem item : command.items()) {
            Lactation lactation = lactationRepository.findByAnimalIdAndTenantIdAndStatut(item.animalId(), command.tenantId(), StatutLactation.EN_COURS)
                    .orElse(null);

            if (lactation != null && item.quantiteKg() != null && item.quantiteKg().signum() > 0) {
                Traite traite = recordMilking.execute(new RecordMilking.Command(
                        command.tenantId(),
                        lactation.getId(),
                        command.auteurId(),
                        command.dateHeure() != null ? command.dateHeure() : now,
                        command.type(),
                        item.quantiteKg()
                ));
                traites.add(traite);
            }
        }

        return traites;
    }

    public record SaisieItem(
            UUID animalId,
            BigDecimal quantiteKg
    ) {}

    public record Command(
            UUID tenantId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            List<SaisieItem> items
    ) {
    }
}
