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
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Enregistrement d'une traite (volume en litres) dans une lactation en cours.
 * <p>
 * Règles : lactation en cours de l'exploitation ; date ni future ni antérieure au début de la lactation ;
 * une seule traite MATIN et une seule traite SOIR par lactation et par jour civil (UTC) — le type AUTRE
 * n'est pas limité ; volume ≥ 0.
 */
@Service
public class RecordMilking {

    /** Tolérance d'horloge entre le téléphone de saisie et le serveur. */
    private static final Duration TOLERANCE_FUTUR = Duration.ofMinutes(10);
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
                .findByIdAndTenantIdAndExploitationId(
                        command.lactationId(),
                        command.tenantId(),
                        command.exploitationId()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lactation introuvable."
                ));

        motifRefus(lactation, command).ifPresent(motif -> {
            throw new IllegalStateException(motif);
        });

        Traite traite = new Traite(
                command.tenantId(),
                command.exploitationId(),
                lactation.getId(),
                command.auteurId(),
                command.dateHeure(),
                command.type(),
                command.quantiteLitres(),
                OffsetDateTime.now()
        );

        Traite saved = traiteRepository.save(traite);

        eventPublisher.publishEvent(
                new MilkingRecorded(
                        saved.getId(),
                        saved.getLactationId(),
                        lactation.getAnimalId(),
                        saved.getTenantId(),
                        saved.getExploitationId(),
                        saved.getQuantiteLitres(),
                        saved.getDateHeure()
                )
        );

        return saved;
    }

    /**
     * Vérifie les règles d'enregistrement sans rien écrire.
     *
     * @return le motif de refus, ou vide si la traite peut être enregistrée.
     */
    @Transactional(readOnly = true)
    public Optional<String> motifRefus(Lactation lactation, Command command) {
        if (lactation.getStatut() != StatutLactation.EN_COURS) {
            return Optional.of("Aucune lactation en cours pour cet animal.");
        }
        if (command.dateHeure() == null) {
            return Optional.of("La date et l'heure de traite sont obligatoires.");
        }
        if (command.dateHeure().isAfter(OffsetDateTime.now().plus(TOLERANCE_FUTUR))) {
            return Optional.of("La date de traite ne peut pas être dans le futur.");
        }
        LocalDate jour = jourCivil(command.dateHeure());
        if (jour.isBefore(lactation.getDateDebut())) {
            return Optional.of("La date de traite ne peut pas être antérieure au début de la lactation ("
                    + lactation.getDateDebut().format(JOUR) + ").");
        }
        if (command.quantiteLitres() == null || command.quantiteLitres().signum() < 0) {
            return Optional.of("La quantité de lait doit être supérieure ou égale à zéro.");
        }
        if (command.type() != null && command.type() != TypeTraite.AUTRE
                && traiteRepository.existsByLactationIdAndTypeAndDateHeureGreaterThanEqualAndDateHeureLessThan(
                        lactation.getId(),
                        command.type(),
                        jour.atStartOfDay().atOffset(ZoneOffset.UTC),
                        jour.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC))) {
            return Optional.of("Une traite " + command.type() + " est déjà enregistrée pour cet animal le "
                    + jour.format(JOUR) + ".");
        }
        return Optional.empty();
    }

    static LocalDate jourCivil(OffsetDateTime dateHeure) {
        return dateHeure.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    public record Command(
            UUID tenantId,
            UUID exploitationId,
            UUID lactationId,
            UUID auteurId,
            OffsetDateTime dateHeure,
            TypeTraite type,
            BigDecimal quantiteLitres
    ) {
    }
}
