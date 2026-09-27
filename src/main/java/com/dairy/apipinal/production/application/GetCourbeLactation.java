package com.dairy.apipinal.production.application;

import com.dairy.apipinal.production.domain.Traite;
import com.dairy.apipinal.production.infrastructure.persistence.TraiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetCourbeLactation {

    private final TraiteRepository traiteRepository;

    public GetCourbeLactation(TraiteRepository traiteRepository) {
        this.traiteRepository = traiteRepository;
    }

    public List<PointCourbe> execute(UUID tenantId, UUID lactationId) {
        List<Traite> traites = traiteRepository.findAllByLactationIdAndTenantIdOrderByDateHeureAsc(lactationId, tenantId);

        // Agréger par jour (LocalDate)
        Map<LocalDate, BigDecimal> productionParJour = traites.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDateHeure().toLocalDate(),
                        TreeMap::new,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                Traite::getQuantiteKg,
                                BigDecimal::add
                        )
                ));

        return productionParJour.entrySet().stream()
                .map(e -> new PointCourbe(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public record PointCourbe(LocalDate date, BigDecimal quantiteKg) {}
}
