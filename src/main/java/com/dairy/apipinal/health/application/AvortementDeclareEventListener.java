package com.dairy.apipinal.health.application;

import com.dairy.apipinal.reproduction.api.AvortementDeclareEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
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

    /**
     * Synchrone, dans la transaction de l'opération d'origine : un échec annule l'ensemble,
     * aucun état intermédiaire incohérent entre modules n'est conservé.
     */
    @EventListener
    public void onAvortementDeclare(AvortementDeclareEvent event) {
        log.info("Réception événement AvortementDeclareEvent pour le cycle {} de l'animal {}", event.cycleId(), event.animalId());

        // Un avortement n'est déclarable qu'une fois par cycle (transition GESTANTE -> AVORTEE) ;
        // la contrainte SQL uk_sante_source_cycle garantit en plus l'unicité de l'événement sanitaire.
        // Aucune exception n'est absorbée : en cas d'échec, la déclaration d'avortement est annulée.
        
        recordHealthEvent.execute(new RecordHealthEvent.Command(
                event.tenantId(),
                event.exploitationId(),
                event.animalId(),
                event.cycleId(),
                event.dateAvortement().atTime(12, 0).atOffset(ZoneOffset.UTC), // Conversion de LocalDate en OffsetDateTime (midi UTC)
                "Avortement déclaré depuis le module Reproduction (Cycle: " + event.cycleId() + ")",
                event.actorId()
        ));
    }
}
