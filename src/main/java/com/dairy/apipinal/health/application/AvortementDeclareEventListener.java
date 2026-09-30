package com.dairy.apipinal.health.application;

import com.dairy.apipinal.reproduction.api.AvortementDeclareEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Component
public class AvortementDeclareEventListener {

    private static final Logger log = LoggerFactory.getLogger(AvortementDeclareEventListener.class);

    private final RecordHealthEvent recordHealthEvent;

    public AvortementDeclareEventListener(RecordHealthEvent recordHealthEvent) {
        this.recordHealthEvent = recordHealthEvent;
    }

    @ApplicationModuleListener
    public void onAvortementDeclare(AvortementDeclareEvent event) {
        log.info("Réception événement AvortementDeclareEvent pour le cycle {} de l'animal {}", event.cycleId(), event.animalId());

        // L'idempotence est gérée soit dans RecordHealthEvent en recherchant un événement similaire à la même date
        // Pour l'instant, on enregistre l'événement sanitaire
        // Note : En production complète, on devrait vérifier si un événement d'avortement existe déjà pour ce cycle
        
        try {
            recordHealthEvent.execute(new RecordHealthEvent.Command(
                    event.tenantId(),
                    event.exploitationId(),
                    event.animalId(),
                    event.cycleId(),
                    event.dateAvortement().atTime(12, 0).atOffset(ZoneOffset.UTC), // Conversion de LocalDate en OffsetDateTime (midi UTC)
                    "Avortement déclaré depuis le module Reproduction (Cycle: " + event.cycleId() + ")",
                    event.tenantId() // actorId temporaire
            ));
        } catch (Exception e) {
            log.warn("L'événement sanitaire d'avortement a peut-être déjà été créé ou a échoué: {}", e.getMessage());
        }
    }
}
