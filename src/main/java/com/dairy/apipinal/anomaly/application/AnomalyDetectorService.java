package com.dairy.apipinal.anomaly.application;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.api.AnomalyQueries;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnomalyDetectorService implements AnomalyQueries {

    private final ProductionQueries productionQueries;
    private final TenantContext tenantContext;

    public AnomalyDetectorService(
            ProductionQueries productionQueries,
            TenantContext tenantContext
    ) {
        this.productionQueries = productionQueries;
        this.tenantContext = tenantContext;
    }

    @Override
    public List<AnomalyAlert> detectAnomalies() {
        List<AnomalyAlert> alerts = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime todayStart = now.minusDays(1);
        OffsetDateTime lastWeekStart = now.minusDays(7);

        BigDecimal todayProd = productionQueries.getTotalMilkProductionKg(todayStart, now);
        BigDecimal lastWeekProd = productionQueries.getTotalMilkProductionKg(lastWeekStart, todayStart);

        BigDecimal avgDailyLastWeek = lastWeekProd.divide(new BigDecimal("6"), 2, RoundingMode.HALF_UP);

        if (avgDailyLastWeek.compareTo(BigDecimal.ZERO) > 0 && todayProd.compareTo(avgDailyLastWeek.multiply(new BigDecimal("0.8"))) < 0) {
            alerts.add(new AnomalyAlert(
                    UUID.randomUUID().toString(),
                    "DROP_MILK_PRODUCTION",
                    "AVERTISSEMENT",
                    "Chute globale de production laitière",
                    "La production laitière des dernières 24h (" + todayProd + " kg) est inférieure de plus de 20% à la moyenne quotidienne de la semaine (" + avgDailyLastWeek + " kg).",
                    null,
                    now
            ));
        }

        return alerts;
    }
}
