package com.dairy.apipinal.production.application;

import com.dairy.apipinal.reproduction.api.VelageEnregistreEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class VelageEnregistreEventListener {

    private static final Logger log = LoggerFactory.getLogger(VelageEnregistreEventListener.class);

    private final StartLactation startLactation;

    public VelageEnregistreEventListener(StartLactation startLactation) {
        this.startLactation = startLactation;
    }

    @EventListener
    public void onVelageEnregistre(VelageEnregistreEvent event) {
        log.info("Réception de l'événement de vêlage pour l'animal {} : démarrage d'une nouvelle lactation.", event.animalId());
        // Pas de capture d'exception : si la lactation ne peut pas démarrer, le vêlage est annulé
        // et l'utilisateur reçoit la cause, au lieu d'un vêlage enregistré sans lactation.
        startLactation.execute(new StartLactation.Command(
                event.tenantId(),
                event.exploitationId(),
                event.animalId(),
                event.cycleId(),
                event.dateVelage(),
                event.actorId()
        ));
        log.info("Nouvelle lactation démarrée pour l'animal {}.", event.animalId());
    }
}
