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
        try {
            startLactation.execute(new StartLactation.Command(
                    event.tenantId(),
                    event.exploitationId(),
                    event.animalId(),
                    event.cycleId(),
                    event.dateVelage(),
                    // Utilisation d'un ID par défaut pour les actions système (ou null si le constructeur l'accepte)
                    event.tenantId()
            ));
            log.info("Nouvelle lactation démarrée avec succès pour l'animal {}.", event.animalId());
        } catch (Exception e) {
            log.error("Impossible de démarrer la lactation suite au vêlage de l'animal {} : {}", event.animalId(), e.getMessage());
        }
    }
}
