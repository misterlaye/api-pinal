package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.nutrition.application.NutritionQueriesImpl;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.domain.UniteAliment;
import com.dairy.apipinal.nutrition.infrastructure.persistence.AlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.PrixAlimentRepository;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DataJpaTest(showSql = false)
@Import({TestcontainersConfiguration.class, NutritionQueriesImpl.class, RationService.class})
class NutritionHistoricalCostIntegrationTest {

    private static final LocalDate PREVIOUS_MONTH = LocalDate.of(2026, 8, 1);
    private static final LocalDate CURRENT_MONTH = LocalDate.of(2026, 9, 1);

    @Autowired private RationRepository rationRepository;
    @Autowired private AlimentRepository alimentRepository;
    @Autowired private PrixAlimentRepository prixAlimentRepository;
    @Autowired private NutritionQueriesImpl queries;
    @Autowired private EntityManager entityManager;

    @MockitoBean private TenantContext tenantContext;
    @MockitoBean private AnimalQueries animalQueries;

    private UUID tenantId;
    private UUID exploitationId;
    private UUID animalId;
    private UUID alimentId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        exploitationId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        when(tenantContext.currentTenantId()).thenReturn(tenantId);
        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        Aliment aliment = alimentRepository.saveAndFlush(new Aliment(
                UUID.randomUUID().toString(), "Foin", "FOURRAGE", UniteAliment.KG
        ));
        alimentId = aliment.getId();
        prixAlimentRepository.saveAndFlush(new PrixAliment(
                alimentId, new BigDecimal("250"), PREVIOUS_MONTH, null
        ));
    }

    @Test
    void closingPreviousRationAndActivatingReplacementPreservesHistoricalCosts() {
        Ration previous = saveRation(tenantId, exploitationId, animalId, PREVIOUS_MONTH, "5", true);
        BigDecimal beforeClosing = queries.calculateFeedCost(animalId, PREVIOUS_MONTH.plusDays(14))
                .orElseThrow().coutAlimentation();
        assertThat(beforeClosing).isEqualByComparingTo("1250");

        previous.terminer(CURRENT_MONTH.minusDays(1));
        rationRepository.saveAndFlush(previous);
        Ration current = saveRation(tenantId, exploitationId, animalId, CURRENT_MONTH, "8", true);
        UUID previousId = previous.getId();
        UUID currentId = current.getId();
        entityManager.clear();

        var historical = queries.calculateFeedCost(animalId, PREVIOUS_MONTH.plusDays(14)).orElseThrow();
        assertThat(historical.rationId()).isEqualTo(previousId);
        assertThat(historical.coutAlimentation()).isEqualByComparingTo(beforeClosing);
        assertThat(queries.findActiveRation(animalId, PREVIOUS_MONTH).orElseThrow().statut())
                .isEqualTo(StatutRation.TERMINEE);
        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH).orElseThrow().coutAlimentation())
                .isEqualByComparingTo("1250");
        assertThat(queries.calculateFeedCost(animalId, CURRENT_MONTH.minusDays(1)).orElseThrow().rationId())
                .isEqualTo(previousId);
        assertThat(queries.calculateFeedCost(animalId, CURRENT_MONTH).orElseThrow().rationId())
                .isEqualTo(currentId);
        assertThat(queries.calculateFeedCost(animalId, CURRENT_MONTH.plusDays(14)).orElseThrow().coutAlimentation())
                .isEqualByComparingTo("2000");
        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH.minusDays(1))).isEmpty();

        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH, CURRENT_MONTH)
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("38750");
        assertThat(queries.calculateTotalFeedCost(PREVIOUS_MONTH, CURRENT_MONTH)
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("38750");
        assertThat(queries.calculateTotalFeedCost(CURRENT_MONTH, CURRENT_MONTH.plusDays(1))
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("2000");
    }

    @Test
    void historicalCostsExcludeDraftsOtherTenantsAndOtherExploitations() {
        Ration effective = saveRation(tenantId, exploitationId, animalId, PREVIOUS_MONTH, "5", true);
        effective.terminer(CURRENT_MONTH.minusDays(1));
        rationRepository.saveAndFlush(effective);
        UUID effectiveId = effective.getId();

        // A draft for the same animal must neither replace nor duplicate the historical ration.
        saveRation(tenantId, exploitationId, animalId, PREVIOUS_MONTH, "100", false);
        saveRation(tenantId, UUID.randomUUID(), UUID.randomUUID(), PREVIOUS_MONTH, "100", true);
        saveRation(UUID.randomUUID(), exploitationId, UUID.randomUUID(), PREVIOUS_MONTH, "100", true);
        entityManager.clear();

        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH).orElseThrow().rationId())
                .isEqualTo(effectiveId);
        assertThat(queries.calculateTotalFeedCost(PREVIOUS_MONTH, PREVIOUS_MONTH.plusDays(1))
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("1250");
        assertThat(queries.calculateFeedCost(animalId, CURRENT_MONTH)).isEmpty();
        assertThat(queries.calculateTotalFeedCost(CURRENT_MONTH, CURRENT_MONTH.plusDays(1))
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("0");

        when(tenantContext.currentExploitationId()).thenReturn(UUID.randomUUID());
        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH)).isEmpty();
        assertThat(queries.calculateTotalFeedCost(PREVIOUS_MONTH, PREVIOUS_MONTH.plusDays(1))
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("0");

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);
        when(tenantContext.currentTenantId()).thenReturn(UUID.randomUUID());
        assertThat(queries.calculateFeedCost(animalId, PREVIOUS_MONTH)).isEmpty();
        assertThat(queries.calculateTotalFeedCost(PREVIOUS_MONTH, PREVIOUS_MONTH.plusDays(1))
                .orElseThrow().coutAlimentation()).isEqualByComparingTo("0");
    }

    private Ration saveRation(UUID tenant, UUID exploitation, UUID animal, LocalDate start,
                              String quantity, boolean activate) {
        Ration ration = new Ration(tenant, exploitation, animal, start, OrigineRation.ACTUELLE);
        ration.ajouterLigne(alimentId, new BigDecimal(quantity));
        if (activate) {
            ration.activer();
        }
        return rationRepository.saveAndFlush(ration);
    }
}
