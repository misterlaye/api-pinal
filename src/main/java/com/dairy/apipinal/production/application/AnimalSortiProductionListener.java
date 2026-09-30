package com.dairy.apipinal.production.application;

import com.dairy.apipinal.animal.api.AnimalSortiEvent;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AnimalSortiProductionListener {

    private static final Logger log = LoggerFactory.getLogger(AnimalSortiProductionListener.class);

    private final LactationRepository lactationRepository;

    public AnimalSortiProductionListener(LactationRepository lactationRepository) {
        this.lactationRepository = lactationRepository;
    }

    @ApplicationModuleListener
    public void onAnimalSorti(AnimalSortiEvent event) {
        log.info("Animal {} sorti ({}). Clôture de lactation active si existante.", event.animalId(), event.nouveauStatut());
        lactationRepository.findByAnimalIdAndTenantIdAndStatut(
                event.animalId(),
                event.tenantId(),
                StatutLactation.EN_COURS
        ).ifPresent(lactation -> {
            LocalDate dateFin = event.dateSortie() != null ? event.dateSortie() : LocalDate.now();
            if (dateFin.isBefore(lactation.getDateDebut())) {
                dateFin = lactation.getDateDebut();
            }
            lactation.terminer(dateFin, event.actorId());
            lactationRepository.save(lactation);
            log.info("Lactation {} clôturée pour l'animal {} suite à sa sortie du troupeau.", lactation.getId(), event.animalId());
        });
    }
}
