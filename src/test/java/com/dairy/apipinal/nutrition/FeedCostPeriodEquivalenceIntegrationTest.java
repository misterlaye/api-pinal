package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.TestcontainersConfiguration;
import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.nutrition.application.CalculateRationCost;
import com.dairy.apipinal.nutrition.application.NutritionQueriesImpl;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.domain.Aliment;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.PrixAliment;
import com.dairy.apipinal.nutrition.domain.Ration;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Le calcul en lot du coût alimentaire de l'exploitation doit rester strictement
 * identique (valeur et échelle) à l'algorithme jour par jour qu'il remplace.
 */
@DataJpaTest(showSql = false)
@Import({TestcontainersConfiguration.class, NutritionQueriesImpl.class, RationService.class})
class FeedCostPeriodEquivalenceIntegrationTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 8, 20);
    private static final LocalDate FIN_EXCLUSIVE = LocalDate.of(2026, 9, 19);

    @Autowired private RationRepository rationRepository;
    @Autowired private AlimentRepository alimentRepository;
    @Autowired private PrixAlimentRepository prixAlimentRepository;
    @Autowired private RationService rationService;
    @Autowired private NutritionQueriesImpl queries;
    @Autowired private EntityManager entityManager;

    @MockitoBean private TenantContext tenantContext;
    @MockitoBean private AnimalQueries animalQueries;

    private UUID tenantId;
    private UUID exploitationId;
    private UUID foin;
    private UUID concentre;
    private UUID mineral;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        exploitationId = UUID.randomUUID();
        when(tenantContext.currentTenantId()).thenReturn(tenantId);
        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        foin = aliment("Foin");
        concentre = aliment("Concentre");
        mineral = aliment("Mineral");

        // Changements de prix au milieu de la période, prix à 4 décimales.
        prix(foin, "145.0000", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 3));
        prix(foin, "155.0000", LocalDate.of(2026, 9, 4), null);
        prix(concentre, "335.1250", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        prix(concentre, "350.0000", LocalDate.of(2026, 9, 1), null);
        prix(mineral, "520.0000", LocalDate.of(2026, 7, 1), null);
    }

    @Test
    void bulkCalculationMatchesDayByDayCalculation() {
        UUID a1 = UUID.randomUUID();
        UUID a2 = UUID.randomUUID();
        UUID a3 = UUID.randomUUID();
        UUID a4 = UUID.randomUUID();

        // Ration ouverte démarrée avant la période.
        ration(a1, LocalDate.of(2026, 8, 1), null, true, line(foin, "6.00"), line(concentre, "5.50"), line(mineral, "0.15"));
        // Ration clôturée en cours de période puis remplacée.
        ration(a2, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 5), true, line(foin, "5.00"), line(concentre, "3.25"));
        ration(a2, LocalDate.of(2026, 9, 6), null, true, line(foin, "7.00"), line(mineral, "0.12"));
        // Ration démarrant en cours de période.
        ration(a3, LocalDate.of(2026, 9, 10), null, true, line(concentre, "2.50"));
        // Ration terminée avant la période : ignorée.
        ration(a4, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 19), true, line(foin, "100"));
        // Ration démarrant le dernier jour exclu : ignorée.
        ration(a4, FIN_EXCLUSIVE, null, true, line(foin, "100"));
        // Brouillon et autre exploitation : ignorés.
        ration(a4, LocalDate.of(2026, 8, 20), null, false, line(foin, "999"));
        Ration autre = new Ration(tenantId, UUID.randomUUID(), UUID.randomUUID(), DEBUT, OrigineRation.ACTUELLE);
        autre.ajouterLigne(foin, new BigDecimal("999"));
        autre.activer();
        rationRepository.saveAndFlush(autre);
        entityManager.clear();

        BigDecimal reference = dayByDayReference(DEBUT, FIN_EXCLUSIVE);
        entityManager.clear();
        BigDecimal bulk = queries.calculateTotalFeedCost(DEBUT, FIN_EXCLUSIVE).orElseThrow().coutAlimentation();

        assertThat(reference.signum()).isPositive();
        assertThat(bulk).isEqualTo(reference); // valeur ET échelle identiques

        for (LocalDate jour = DEBUT; jour.isBefore(FIN_EXCLUSIVE); jour = jour.plusDays(1)) {
            entityManager.clear();
            assertThat(queries.calculateTotalFeedCost(jour, jour.plusDays(1)).orElseThrow().coutAlimentation())
                    .as("jour %s", jour)
                    .isEqualTo(dayByDayReference(jour, jour.plusDays(1)));
        }
    }

    @Test
    void periodWithoutRationCostsZero() {
        assertThat(queries.calculateTotalFeedCost(DEBUT, FIN_EXCLUSIVE).orElseThrow().coutAlimentation())
                .isEqualTo(dayByDayReference(DEBUT, FIN_EXCLUSIVE))
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void missingPriceStillFailsLikeDayByDayCalculation() {
        UUID sansPrix = aliment("SansPrixAvantSeptembre");
        prix(sansPrix, "10.0000", LocalDate.of(2026, 9, 1), null);
        ration(UUID.randomUUID(), DEBUT, null, true, line(sansPrix, "1"));
        entityManager.clear();

        assertThatThrownBy(() -> dayByDayReference(DEBUT, FIN_EXCLUSIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun prix applicable pour l'aliment " + sansPrix + " à la date " + DEBUT);
        assertThatThrownBy(() -> queries.calculateTotalFeedCost(DEBUT, FIN_EXCLUSIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun prix applicable pour l'aliment " + sansPrix + " à la date " + DEBUT);
    }

    /** Algorithme d'origine : une requête de rations par jour, puis calculateCost par ration. */
    private BigDecimal dayByDayReference(LocalDate debut, LocalDate finExclusive) {
        BigDecimal total = BigDecimal.ZERO;
        for (LocalDate date = debut; date.isBefore(finExclusive); date = date.plusDays(1)) {
            for (Ration ration : rationRepository.findAllEffectiveRationsAtDate(tenantId, exploitationId, date)) {
                total = total.add(rationService.calculateCost(
                        new CalculateRationCost(ration.getAnimalId(), ration.getId(), date)
                ).coutTotal());
            }
        }
        return total;
    }

    private record Line(UUID alimentId, String quantite) {}

    private static Line line(UUID alimentId, String quantite) {
        return new Line(alimentId, quantite);
    }

    private void ration(UUID animalId, LocalDate debut, LocalDate fin, boolean activate, Line... lines) {
        Ration ration = new Ration(tenantId, exploitationId, animalId, debut, OrigineRation.ACTUELLE);
        for (Line line : lines) {
            ration.ajouterLigne(line.alimentId(), new BigDecimal(line.quantite()));
        }
        if (activate) {
            ration.activer();
            if (fin != null) {
                ration.terminer(fin);
            }
        }
        rationRepository.saveAndFlush(ration);
    }

    private UUID aliment(String nom) {
        return alimentRepository.saveAndFlush(new Aliment(
                UUID.randomUUID().toString(), nom, "FOURRAGE", UniteAliment.KG
        )).getId();
    }

    private void prix(UUID alimentId, String prix, LocalDate debut, LocalDate fin) {
        prixAlimentRepository.saveAndFlush(new PrixAliment(alimentId, new BigDecimal(prix), debut, fin));
    }
}
