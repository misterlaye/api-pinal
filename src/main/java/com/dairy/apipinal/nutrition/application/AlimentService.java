package com.dairy.apipinal.nutrition.application;

import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AlimentService {

    private final AlimentRepository alimentRepository;

    public AlimentService(AlimentRepository alimentRepository) {
        this.alimentRepository = alimentRepository;
    }

    @Transactional
    public Aliment create(CreateAliment command) {
        if (alimentRepository.existsByCode(command.code())) {
            throw new IllegalArgumentException("Un aliment avec ce code existe déjà.");
        }

        Aliment aliment = new Aliment(
                command.code(),
                command.nom(),
                command.categorie(),
                command.unite()
        );

        return alimentRepository.save(aliment);
    }

    @Transactional(readOnly = true)
    public List<Aliment> getAll() {
        return alimentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Aliment getById(UUID id) {
        return alimentRepository.findByIdAndActifTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Aliment introuvable"));
    }
}
