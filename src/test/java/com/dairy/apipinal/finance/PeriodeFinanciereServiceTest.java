package com.dairy.apipinal.finance;

import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.finance.application.CalculateRentabilite;
import com.dairy.apipinal.finance.application.ChargeExploitationService;
import com.dairy.apipinal.finance.application.PeriodeFinanciereService;
import com.dairy.apipinal.finance.application.RentabiliteService;
import com.dairy.apipinal.finance.domain.PrixVenteLait;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.finance.infrastructure.persistence.PrixVenteLaitRepository;
import com.dairy.apipinal.nutrition.api.ExploitationFeedCostReference;
import com.dairy.apipinal.nutrition.api.NutritionQueries;
import com.dairy.apipinal.nutrition.api.PrixAlimentManquant;
import com.dairy.apipinal.production.api.ProductionQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Règles du calcul financier de période (voir REGLES-CALCUL.md).
 */
@ExtendWith(MockitoExtension.class)
class PeriodeFinanciereServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID EXPLOITATION = UUID.randomUUID();
    private static final LocalDate J1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate J2 = LocalDate.of(2026, 9, 2);
    private static final LocalDate J3 = LocalDate.of(2026, 9, 3);

    @Mock private ProductionQueries productionQueries;
    @Mock private NutritionQueries nutritionQueries;
    @Mock private PrixVenteLaitRepository prixRepository;
    @Mock private ChargeExploitationService chargeService;
    @Mock private TenantContext tenantContext;
    @Mock private CalculRentabiliteRepository calculRepository;

    private PeriodeFinanciereService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.currentTenantId()).thenReturn(TENANT);
        lenient().when(tenantContext.currentExploitationId()).thenReturn(EXPLOITATION);
        service = new PeriodeFinanciereService(productionQueries, nutritionQueries, prixRepository, chargeService, tenantContext);
    }

    @Test
    void valuesEachDayAtTheApplicablePriceAndIncludesAllCosts() {
        lait(Map.of(J1, "10", J2, "20"));
        when(prixRepository.findAllOverlappingPeriod(TENANT, EXPLOITATION, J1, J3)).thenReturn(List.of(
                new PrixVenteLait(TENANT, EXPLOITATION, new BigDecimal("600"), J1, J1),
                new PrixVenteLait(TENANT, EXPLOITATION, new BigDecimal("650"), J2, null)
        ));
        alimentation("5000", List.of());
        when(chargeService.sumAutresCharges(J1, J3.plusDays(1))).thenReturn(new BigDecimal("1000"));

        ResultatFinancier r = service.calculer(J1, J3);

        assertThat(r.volumeLitres()).isEqualByComparingTo("30");
        assertThat(r.chiffreAffaires()).isEqualByComparingTo("19000");      // 10×600 + 20×650
        assertThat(r.prixMoyenParLitre()).isEqualByComparingTo("633.33");
        assertThat(r.coutAlimentation()).isEqualByComparingTo("5000");
        assertThat(r.autresCharges()).isEqualByComparingTo("1000");
        assertThat(r.coutTotal()).isEqualByComparingTo("6000");
        assertThat(r.coutRevientParLitre()).isEqualByComparingTo("200");
        assertThat(r.marge()).isEqualByComparingTo("13000");
        assertThat(r.tauxMarge()).isEqualByComparingTo("68.42");
        assertThat(r.avertissements()).isEmpty();
        assertThat(r.complet()).isTrue();
    }

    @Test
    void missingMilkPriceMakesRevenueAndMarginUnknownNeverZero() {
        lait(Map.of(J1, "10", J2, "20"));
        when(prixRepository.findAllOverlappingPeriod(TENANT, EXPLOITATION, J1, J3)).thenReturn(List.of(
                new PrixVenteLait(TENANT, EXPLOITATION, new BigDecimal("600"), J1, J1)
        ));
        alimentation("5000", List.of());
        when(chargeService.sumAutresCharges(any(), any())).thenReturn(BigDecimal.ZERO);

        ResultatFinancier r = service.calculer(J1, J3);

        assertThat(r.volumeLitres()).isEqualByComparingTo("30");
        assertThat(r.chiffreAffaires()).isNull();
        assertThat(r.marge()).isNull();
        assertThat(r.prixMoyenParLitre()).isNull();
        assertThat(r.coutTotal()).isEqualByComparingTo("5000");
        assertThat(r.avertissements()).extracting(ResultatFinancier.Avertissement::code).containsExactly("PRIX_LAIT_MANQUANT");
        assertThat(r.avertissements().getFirst().message()).contains("02/09/2026");
    }

    @Test
    void missingFeedPriceMakesFeedCostAndDependentValuesUnknown() {
        lait(Map.of(J1, "10"));
        when(prixRepository.findAllOverlappingPeriod(TENANT, EXPLOITATION, J1, J3)).thenReturn(List.of(
                new PrixVenteLait(TENANT, EXPLOITATION, new BigDecimal("600"), J1, null)
        ));
        alimentation("800", List.of(new PrixAlimentManquant(UUID.randomUUID(), "Foin", J2, J3)));
        when(chargeService.sumAutresCharges(any(), any())).thenReturn(new BigDecimal("100"));

        ResultatFinancier r = service.calculer(J1, J3);

        assertThat(r.chiffreAffaires()).isEqualByComparingTo("6000");
        assertThat(r.coutAlimentation()).isNull();
        assertThat(r.coutAlimentationConnu()).isEqualByComparingTo("800");
        assertThat(r.coutTotal()).isNull();
        assertThat(r.coutRevientParLitre()).isNull();
        assertThat(r.marge()).isNull();
        assertThat(r.avertissements()).extracting(ResultatFinancier.Avertissement::message)
                .anyMatch(m -> m.contains("Foin") && m.contains("du 02/09/2026 au 03/09/2026"));
    }

    @Test
    void noMilkGivesZeroRevenueButNoCostPerLitre() {
        lait(Map.of());
        when(prixRepository.findAllOverlappingPeriod(TENANT, EXPLOITATION, J1, J3)).thenReturn(List.of());
        alimentation("300", List.of());
        when(chargeService.sumAutresCharges(any(), any())).thenReturn(new BigDecimal("200"));

        ResultatFinancier r = service.calculer(J1, J3);

        assertThat(r.volumeLitres()).isZero();
        assertThat(r.chiffreAffaires()).isZero();
        assertThat(r.coutTotal()).isEqualByComparingTo("500");
        assertThat(r.marge()).isEqualByComparingTo("-500");
        assertThat(r.coutRevientParLitre()).isNull();
        assertThat(r.prixMoyenParLitre()).isNull();
        assertThat(r.tauxMarge()).isNull();
        assertThat(r.avertissements()).extracting(ResultatFinancier.Avertissement::code).containsExactly("AUCUNE_TRAITE");
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThatThrownBy(() -> service.calculer(J3, J1)).hasMessageContaining("période");
        verifyNoInteractions(productionQueries);
    }

    @Test
    void snapshotIsRefusedWhenDataIsIncomplete() {
        lait(Map.of(J1, "10"));
        when(prixRepository.findAllOverlappingPeriod(TENANT, EXPLOITATION, J1, J3)).thenReturn(List.of());
        alimentation("0", List.of());
        when(chargeService.sumAutresCharges(any(), any())).thenReturn(BigDecimal.ZERO);
        RentabiliteService rentabilite = new RentabiliteService(service, calculRepository, tenantContext);

        assertThatThrownBy(() -> rentabilite.calculate(new CalculateRentabilite(J1, J3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prix de vente du lait manquant");
        verifyNoInteractions(calculRepository);
    }

    private void lait(Map<LocalDate, String> parJour) {
        Map<LocalDate, BigDecimal> valeurs = new TreeMap<>();
        parJour.forEach((jour, litres) -> valeurs.put(jour, new BigDecimal(litres)));
        when(productionQueries.getDailyMilkProductionLitres(J1, J3)).thenReturn(valeurs);
    }

    private void alimentation(String connu, List<PrixAlimentManquant> manquants) {
        BigDecimal cout = new BigDecimal(connu);
        when(nutritionQueries.calculateTotalFeedCost(J1, J3.plusDays(1))).thenReturn(Optional.of(
                new ExploitationFeedCostReference(J1, J3.plusDays(1), manquants.isEmpty() ? cout : null, cout, manquants)));
    }
}
