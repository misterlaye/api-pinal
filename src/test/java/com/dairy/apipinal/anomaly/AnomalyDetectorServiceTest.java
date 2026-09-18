package com.dairy.apipinal.anomaly;

import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import com.dairy.apipinal.anomaly.application.AnomalyDetectorService;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectorServiceTest {

    @Mock
    private ProductionQueries productionQueries;

    @Mock
    private TenantContext tenantContext;

    private AnomalyDetectorService anomalyDetectorService;

    @BeforeEach
    void setUp() {
        anomalyDetectorService = new AnomalyDetectorService(productionQueries, tenantContext);
    }

    @Test
    void shouldDetectDropInMilkProduction() {
        // Production aujourd'hui = 10kg, production semaine passée = 600kg (moyenne 100kg/j) -> chute > 20%
        when(productionQueries.getTotalMilkProductionKg(any(), any()))
                .thenReturn(new BigDecimal("10.00"))
                .thenReturn(new BigDecimal("600.00"));

        List<AnomalyAlert> alerts = anomalyDetectorService.detectAnomalies();

        assertFalse(alerts.isEmpty());
        assertEquals("DROP_MILK_PRODUCTION", alerts.get(0).code());
        assertEquals("AVERTISSEMENT", alerts.get(0).severite());
    }

    @Test
    void shouldNotDetectAnomalyWhenProductionIsStable() {
        // Production aujourd'hui = 100kg, semaine passée = 600kg (moyenne 100kg/j)
        when(productionQueries.getTotalMilkProductionKg(any(), any()))
                .thenReturn(new BigDecimal("100.00"))
                .thenReturn(new BigDecimal("600.00"));

        List<AnomalyAlert> alerts = anomalyDetectorService.detectAnomalies();

        assertTrue(alerts.isEmpty());
    }
}
