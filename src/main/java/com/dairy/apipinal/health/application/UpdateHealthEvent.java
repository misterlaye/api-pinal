package com.dairy.apipinal.health.application;

import com.dairy.apipinal.health.domain.EvenementSanitaire;
import com.dairy.apipinal.health.infrastructure.persistence.EvenementSanitaireRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class UpdateHealthEvent {

    private final EvenementSanitaireRepository repository;

    public UpdateHealthEvent(
            EvenementSanitaireRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public EvenementSanitaire execute(Command command) {

        EvenementSanitaire event = repository
                .findByIdAndTenantId(command.eventId(), command.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Événement sanitaire introuvable."));

        event.update(
                command.description(),
                command.diagnostic(),
                command.traitement(),
                command.dateFin(),
                command.actorId()
        );

        return event;
    }

    public record Command(
            UUID eventId,
            UUID tenantId,
            String description,
            String diagnostic,
            String traitement,
            LocalDate dateFin,
            UUID actorId
    ) {
    }
}