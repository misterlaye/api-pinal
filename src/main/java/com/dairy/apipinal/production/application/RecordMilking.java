package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.api.event.MilkingRecorded;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.domain.TypeTraite;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import com.dairy.apipinal.production.infrastructure.persistence.TraiteRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RecordMilking {

    private final LactationRepository lactationRepository;
    private final TraiteRepository traiteRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RecordMilking(
            LactationRepository lactationRepository,
            TraiteRepository traiteRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.lactationRepository = lactationRepository;
        this.traiteRepository = traiteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Traite execute(Command command) {

        Lactation lactation = lactationRepository
                .findByIdAndTenantId(
                        command.lactationId(),
                        command.tenantId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lactation introuvable."
                ));

        if (lactation.getStatut() != StatutLactation.EN_COURS) {
            throw new IllegalStateException(
                    "Une traite ne peut appartenir qu'à une lactation en cours."
            );
        }

        if (command.dateHeure().toLocalDate().isBefore(
                lactation.getDateDebut()
        )) {
            throw new IllegalArgumentException(
                    "La date de traite ne peut pas être antérieure au début de la lactation."
            );
        }

        Traite traite = new Traite(
                command.tenantId(),
                lactation.getId(),
                command.auteurId(),
                command.dateHeure(),
                command.type(),
                command.quantiteKg(),
                OffsetDateTime.now()
        );

        Traite saved = traiteRepository.save(traite);

        eventPublisher.publishEvent(
                new MilkingRecorded(
                        saved.getId(),
                        saved.getLactationId(),
                        lactation.getAnimalId(),
                        saved.getTenantId(),
                        saved.getQuantiteKg(),
                        saved.getDateHeure()
                )
        );

        return saved;
    }

    public record Command(
            UUID tenantId,
            UUID lactationId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            BigDecimal quantiteKg
    ) {
    }
}