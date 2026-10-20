package com.dairy.apipinal.reproduction.application;

import com.dairy.apipinal.animal.api.AnimalSortiEvent;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.domain.StatutReproduction;
import com.dairy.apipinal.reproduction.infrastructure.persistence.CycleReproductionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnimalSortiReproductionListener {

    private static final Logger log = LoggerFactory.getLogger(AnimalSortiReproductionListener.class);

    private final CycleReproductionRepository cycleRepository;

    public AnimalSortiReproductionListener(CycleReproductionRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    /**
     * Synchrone, dans la transaction de l'opération d'origine : un échec annule l'ensemble,
     * aucun état intermédiaire incohérent entre modules n'est conservé.
     */
    @EventListener
    public void onAnimalSorti(AnimalSortiEvent event) {
        log.info("Animal {} sorti ({}). Interruption des cycles de reproduction en cours.", event.animalId(), event.nouveauStatut());
        List<CycleReproduction> cycles = cycleRepository.findByAnimalIdAndTenantIdAndExploitationIdOrderByNumeroCycleDesc(
                event.animalId(), event.tenantId(), event.exploitationId());
        for (CycleReproduction cycle : cycles) {
            if (cycle.getStatut() == StatutReproduction.EN_ATTENTE_CONSTAT || cycle.getStatut() == StatutReproduction.GESTANTE) {
                cycle.interrompre(event.actorId());
                cycleRepository.save(cycle);
                log.info("Cycle {} interrompu pour l'animal {} suite à sa sortie du troupeau.", cycle.getId(), event.animalId());
            }
        }
    }
}
