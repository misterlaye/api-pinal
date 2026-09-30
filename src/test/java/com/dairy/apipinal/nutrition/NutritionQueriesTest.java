package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.nutrition.api.RationCostReference;
import com.dairy.apipinal.nutrition.api.RationReference;
import com.dairy.apipinal.nutrition.application.NutritionQueriesImpl;
import com.dairy.apipinal.nutrition.application.RationCostResult;
import com.dairy.apipinal.nutrition.application.RationService;
import com.dairy.apipinal.nutrition.application.CalculateRationCost;
import com.dairy.apipinal.nutrition.domain.OrigineRation;
import com.dairy.apipinal.nutrition.domain.Ration;
import com.dairy.apipinal.nutrition.domain.StatutRation;
import com.dairy.apipinal.nutrition.infrastructure.persistence.RationRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NutritionQueriesTest {

    @Mock
    private RationRepository rationRepository;

    @Mock
    private RationService rationService;

    @Mock
    private TenantContext tenantContext;

    private NutritionQueriesImpl nutritionQueries;

    @BeforeEach
    void setUp() {
        nutritionQueries = new NutritionQueriesImpl(
                rationRepository,
                rationService,
                tenantContext
        );
    }

    @Test
    void shouldGetRationForCurrentTenant() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID rationId = UUID.randomUUID();
        UUID animalId = UUID.randomUUID();

        LocalDate dateDebut = LocalDate.of(2026, 9, 1);
        LocalDate dateFin = LocalDate.of(2026, 9, 30);

        Ration ration = new Ration(tenantId, exploitationId,
                animalId,
                dateDebut,
                OrigineRation.ACTUELLE
        );

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(Optional.of(ration));

        RationReference result =
                nutritionQueries.getRation(rationId);

        assertThat(result.id())
                .isEqualTo(ration.getId());

        assertThat(result.tenantId())
                .isEqualTo(tenantId);

        assertThat(result.animalId())
                .isEqualTo(animalId);

        assertThat(result.dateDebut())
                .isEqualTo(dateDebut);

        assertThat(result.dateFin()).isNull();

        assertThat(result.statut())
                .isEqualTo(StatutRation.BROUILLON);

        assertThat(result.origine())
                .isEqualTo(OrigineRation.ACTUELLE);
    }

    @Test
    void shouldReturnFalseWhenRationDoesNotExistForCurrentTenant() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.existsByIdAndTenantId(
                rationId,
                tenantId
        )).thenReturn(false);

        boolean result =
                nutritionQueries.exists(rationId);

        assertThat(result)
                .isFalse();
    }

    @Test
    void shouldFindActiveRationAtDate() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 9, 15);
        LocalDate dateDebut = LocalDate.of(2026, 9, 1);

        Ration ration = new Ration(tenantId, exploitationId,
                animalId,
                dateDebut,
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(UUID.randomUUID(), new BigDecimal("5"));
        ration.activer();

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findEffectiveRationAtDate(tenantId, exploitationId, animalId, date)).thenReturn(Optional.of(ration));

        Optional<RationReference> result =
                nutritionQueries.findActiveRation(
                        animalId,
                        date
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().animalId())
                .isEqualTo(animalId);

        assertThat(result.get().statut())
                .isEqualTo(StatutRation.ACTIVE);

        assertThat(result.get().origine())
                .isEqualTo(OrigineRation.ACTUELLE);

        verify(rationRepository)
                .findEffectiveRationAtDate(tenantId, exploitationId, animalId, date);
    }

    @Test
    void shouldReturnEmptyWhenNoActiveRationExistsAtDate() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID animalId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 15);

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findEffectiveRationAtDate(tenantId, exploitationId, animalId, date)).thenReturn(Optional.empty());

        Optional<RationReference> result =
                nutritionQueries.findActiveRation(
                        animalId,
                        date
                );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldNotExposeRationFromAnotherTenant() {

        UUID currentTenantId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        when(tenantContext.currentTenantId())
                .thenReturn(currentTenantId);

        when(rationRepository.findByIdAndTenantId(
                rationId,
                currentTenantId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                nutritionQueries.getRation(rationId)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ration introuvable");

        verify(rationRepository)
                .findByIdAndTenantId(
                        rationId,
                        currentTenantId
                );
    }

    @Test
    void shouldCalculateFeedCostThroughRationService() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 9, 15);

        Ration ration = new Ration(tenantId, exploitationId,
                animalId,
                LocalDate.of(2026, 9, 1),
                OrigineRation.ACTUELLE
        );

        ration.ajouterLigne(UUID.randomUUID(), new BigDecimal("5"));
        ration.activer();

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findEffectiveRationAtDate(tenantId, exploitationId, animalId, date)).thenReturn(Optional.of(ration));

        var costResult =
                new com.dairy.apipinal.nutrition.application.RationCostResult(
                        rationId,
                        date,
                        java.util.List.of(),
                        new BigDecimal("1250.00")
                );

        when(rationService.calculateCost(
                new CalculateRationCost(
                        animalId,
                        ration.getId(),
                        date
                )
        )).thenReturn(costResult);

        Optional<RationCostReference> result =
                nutritionQueries.calculateFeedCost(
                        animalId,
                        date
                );

        assertThat(result)
                .isPresent();

        assertThat(result.get().rationId())
                .isEqualTo(rationId);

        assertThat(result.get().animalId())
                .isEqualTo(animalId);

        assertThat(result.get().dateCalcul())
                .isEqualTo(date);

        assertThat(result.get().coutAlimentation())
                .isEqualByComparingTo("1250.00");

        verify(rationService)
                .calculateCost(
                        new CalculateRationCost(
                                animalId,
                                ration.getId(),
                                date
                        )
                );
    }

    @Test
    void shouldReturnEmptyFeedCostWhenNoActiveRationExists() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID animalId = UUID.randomUUID();

        LocalDate date = LocalDate.of(2026, 9, 15);

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findEffectiveRationAtDate(tenantId, exploitationId, animalId, date)).thenReturn(Optional.empty());

        Optional<RationCostReference> result =
                nutritionQueries.calculateFeedCost(
                        animalId,
                        date
                );

        assertThat(result)
                .isEmpty();

        verify(
                rationService,
                never()
        ).calculateCost(any());
    }

    @Test
    void shouldCalculateTotalFeedCostForExploitation() {

        UUID tenantId = UUID.randomUUID();
        UUID exploitationId = UUID.fromString("d2c88f9a-1111-4b11-1111-d11111111111");
        UUID animalId = UUID.randomUUID();
        UUID rationId = UUID.randomUUID();

        LocalDate jour1 = LocalDate.of(2026, 9, 1);
        LocalDate jour2 = LocalDate.of(2026, 9, 2);
        LocalDate finExclusive = LocalDate.of(2026, 9, 3);

        Ration ration =
                mock(Ration.class);

        when(ration.getId())
                .thenReturn(rationId);

        when(ration.getAnimalId())
                .thenReturn(animalId);

        when(tenantContext.currentExploitationId()).thenReturn(exploitationId);

        when(tenantContext.currentTenantId())
                .thenReturn(tenantId);

        when(rationRepository.findAllEffectiveRationsAtDate(tenantId, exploitationId, jour1)).thenReturn(List.of(ration));

        when(rationRepository.findAllEffectiveRationsAtDate(tenantId, exploitationId, jour2)).thenReturn(List.of(ration));

        when(rationService.calculateCost(any()))
                .thenReturn(
                        mockRationCostResult(
                                rationId,
                                jour1,
                                new BigDecimal("3500.00")
                        ),
                        mockRationCostResult(
                                rationId,
                                jour2,
                                new BigDecimal("3700.00")
                        )
                );

        NutritionQueriesImpl queries =
                new NutritionQueriesImpl(
                        rationRepository,
                        rationService,
                        tenantContext
                );

        var result =
                queries.calculateTotalFeedCost(
                        jour1,
                        finExclusive
                );

        assertTrue(result.isPresent());

        assertEquals(
                new BigDecimal("7200.00"),
                result.get().coutAlimentation()
        );
    }

    @Test
    void shouldRejectHistoricalCostWithoutCurrentExploitation() {
        when(tenantContext.currentTenantId()).thenReturn(UUID.randomUUID());
        LocalDate date = LocalDate.of(2026, 8, 15);

        assertThatThrownBy(() -> nutritionQueries.calculateFeedCost(UUID.randomUUID(), date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exploitation courante");
        assertThatThrownBy(() -> nutritionQueries.calculateTotalFeedCost(date, date.plusDays(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exploitation courante");
        verifyNoInteractions(rationRepository, rationService);
    }

    private RationCostResult mockRationCostResult(
            UUID rationId,
            LocalDate date,
            BigDecimal cout
    ) {
        return new RationCostResult(
                rationId,
                date,
                List.of(),
                cout
        );
    }
}
