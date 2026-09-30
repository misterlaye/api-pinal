package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.animal.api.AnimalSortiEvent;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AnimalSortiNutritionListener {

    private static final Logger log = LoggerFactory.getLogger(AnimalSortiNutritionListener.class);

    private final RationRepository rationRepository;

    public AnimalSortiNutritionListener(RationRepository rationRepository) {
        this.rationRepository = rationRepository;
    }

    @ApplicationModuleListener
    public void onAnimalSorti(AnimalSortiEvent event) {
        log.info("Animal {} sorti ({}). Clôture des rations actives.", event.animalId(), event.nouveauStatut());
        List<Ration> activeRations = rationRepository.findByAnimalIdAndTenantIdAndStatut(
                event.animalId(),
                event.tenantId(),
                StatutRation.ACTIVE
        );
        for (Ration ration : activeRations) {
            LocalDate dateFin = event.dateSortie() != null ? event.dateSortie() : LocalDate.now();
            if (dateFin.isBefore(ration.getDateDebut())) {
                dateFin = ration.getDateDebut();
            }
            ration.terminer(dateFin);
            rationRepository.save(ration);
            log.info("Ration {} terminée pour l'animal {} suite à sa sortie du troupeau.", ration.getId(), event.animalId());
        }
    }
}
