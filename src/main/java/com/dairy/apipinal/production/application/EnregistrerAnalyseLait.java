package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.AnalyseLait;
import com.dairy.apipinal.production.domain.AnalyseLaitRepository;
import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.infrastructure.persistence.LactationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class EnregistrerAnalyseLait {

    private final AnalyseLaitRepository analyseLaitRepository;
    private final LactationRepository lactationRepository;

    public EnregistrerAnalyseLait(
            AnalyseLaitRepository analyseLaitRepository,
            LactationRepository lactationRepository
    ) {
        this.analyseLaitRepository = analyseLaitRepository;
        this.lactationRepository = lactationRepository;
    }

    @Transactional
    public AnalyseLait execute(Command command) {
        Lactation lactation = lactationRepository.findById(command.lactationId())
                .filter(l -> l.getTenantId().equals(command.tenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Lactation introuvable."));

        AnalyseLait analyse = new AnalyseLait(
                command.tenantId(),
                lactation.getExploitationId(),
                lactation.getId(),
                command.auteurId(),
                command.dateAnalyse(),
                command.tauxButyreux(),
                command.tauxProteique(),
                command.cellulesSomatiques(),
                OffsetDateTime.now()
        );

        return analyseLaitRepository.save(analyse);
    }

    public record Command(
            UUID tenantId,
            UUID lactationId,
            UUID auteurId,
            LocalDate dateAnalyse,
            BigDecimal tauxButyreux,
            BigDecimal tauxProteique,
            Integer cellulesSomatiques
    ) {
    }
}
