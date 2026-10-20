package com.dairy.apipinal.finance;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.finance.application.CalculateRentabilite;
import com.dairy.apipinal.finance.application.RentabiliteService;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Le snapshot de démonstration (septembre 2026) doit être exactement reconstructible
 * à partir des données sources avec les règles de calcul en vigueur.
 * Base Testcontainers avec le profil "demo" : n'utilise jamais la base de développement.
 * Exclu de "mvn test" (suffixe IT) : ./mvnw test -Dtest=SnapshotReconstructibilityIT
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles({"test", "demo"})
class SnapshotReconstructibilityIT {

    private static final UUID DEMO_TENANT = UUID.fromString("b0000000-0000-0000-0000-000000000001");
    private static final UUID DEMO_EXPLOITATION = UUID.fromString("b0000000-0000-0000-0000-000000000003");

    @Autowired
    private RentabiliteService rentabiliteService;

    @Autowired
    private CalculRentabiliteRepository snapshotRepository;

    @MockitoBean
    private TenantContext tenantContext;

    @Test
    void demoSnapshotIsReconstructibleFromSourceData() {
        when(tenantContext.currentTenantId()).thenReturn(DEMO_TENANT);
        when(tenantContext.currentExploitationId()).thenReturn(DEMO_EXPLOITATION);

        LocalDate debut = LocalDate.of(2026, 9, 1);
        LocalDate fin = LocalDate.of(2026, 9, 30);

        CalculRentabilite live = rentabiliteService.calculateLive(new CalculateRentabilite(debut, fin));
        CalculRentabilite snapshot = snapshotRepository
                .findAllByTenantIdAndExploitationIdOrderByDateCalculDesc(DEMO_TENANT, DEMO_EXPLOITATION)
                .stream()
                .filter(s -> s.getPeriodeDebut().equals(debut))
                .findFirst()
                .orElseThrow();

        assertThat(scale(live.getVolumeLait())).isEqualByComparingTo(scale(snapshot.getVolumeLait()));
        assertThat(scale(live.getChiffreAffaires())).isEqualByComparingTo(scale(snapshot.getChiffreAffaires()));
        assertThat(scale(live.getCoutAlimentation())).isEqualByComparingTo(scale(snapshot.getCoutAlimentation()));
        assertThat(scale(live.getAutresCharges())).isEqualByComparingTo(scale(snapshot.getAutresCharges()));
        assertThat(scale(live.getMarge())).isEqualByComparingTo(scale(snapshot.getMarge()));
    }

    private static java.math.BigDecimal scale(java.math.BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
