package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.application.CreatePrixAliment;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.PrixAlimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrixAlimentService {

    private final AlimentRepository alimentRepository;
    private final PrixAlimentRepository prixAlimentRepository;

    public PrixAlimentService(
            AlimentRepository alimentRepository,
            PrixAlimentRepository prixAlimentRepository
    ) {
        this.alimentRepository = alimentRepository;
        this.prixAlimentRepository = prixAlimentRepository;
    }

    @Transactional
    public PrixAliment create(CreatePrixAliment command) {

        alimentRepository.findById(command.alimentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aliment introuvable."
                ));

        if (command.dateFin() != null
                && command.dateFin().isBefore(command.dateDebut())) {
            throw new IllegalArgumentException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }

        boolean overlapping;

        if (command.dateFin() != null) {
            overlapping =
                    prixAlimentRepository.existsOverlappingPeriodWithEndDate(
                            command.alimentId(),
                            command.dateDebut(),
                            command.dateFin()
                    );
        } else {
            overlapping =
                    prixAlimentRepository.existsOverlappingOpenEndedPeriod(
                            command.alimentId(),
                            command.dateDebut()
                    );
        }

        if (overlapping) {
            throw new IllegalStateException(
                    "La période du prix chevauche une période existante."
            );
        }

        PrixAliment prixAliment = new PrixAliment(
                command.alimentId(),
                command.prixUnitaire(),
                command.dateDebut(),
                command.dateFin()
        );

        return prixAlimentRepository.save(prixAliment);
    }

    @Transactional(readOnly = true)
    public List<PrixAliment> getHistory(
            GetPrixAlimentHistory query
    ) {
        alimentRepository.findById(query.alimentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aliment introuvable."
                ));

        return prixAlimentRepository
                .findAllByAlimentIdOrderByDateDebutDesc(
                        query.alimentId()
                );
    }
}