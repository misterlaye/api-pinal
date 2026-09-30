package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.api.AnimalRentabilityReference;
import com.dairy.apipinal.finance.api.FinanceQueries;
import com.dairy.apipinal.finance.api.RentabiliteReference;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse;
import com.dairy.apipinal.finance.infrastructure.web.FinanceDashboardResponse.*;
import com.dairy.apipinal.finance.infrastructure.web.TransactionResponse;
import com.dairy.apipinal.finance.domain.ChargeExploitation;
import com.dairy.apipinal.finance.infrastructure.persistence.ChargeExploitationRepository;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class FinanceQueriesImpl implements FinanceQueries {

    private final AnimalRentabilityService animalRentabilityService;
    private final RentabiliteService rentabiliteService;
    private final CalculRentabiliteRepository calculRentabiliteRepository;
    private final ChargeExploitationRepository chargeRepository;
    private final com.dairy.apipinal.production.api.ProductionQueries productionQueries;
    private final TenantContext tenantContext;

    public FinanceQueriesImpl(
            AnimalRentabilityService animalRentabilityService,
            RentabiliteService rentabiliteService,
            CalculRentabiliteRepository calculRentabiliteRepository,
            ChargeExploitationRepository chargeRepository,
            com.dairy.apipinal.production.api.ProductionQueries productionQueries,
            TenantContext tenantContext
    ) {
        this.animalRentabilityService = animalRentabilityService;
        this.rentabiliteService = rentabiliteService;
        this.calculRentabiliteRepository = calculRentabiliteRepository;
        this.chargeRepository = chargeRepository;
        this.productionQueries = productionQueries;
        this.tenantContext = tenantContext;
    }

    @Override
    public Optional<AnimalRentabilityReference> calculateAnimalRentability(
            UUID animalId,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        return animalRentabilityService
                .calculate(animalId, dateDebut, dateFin)
                .map(this::toAnimalReference);
    }

    @Override
    public Optional<RentabiliteReference> findLatestRentabilite() {

        return calculRentabiliteRepository
                .findFirstByTenantIdOrderByDateCalculDesc(
                        tenantContext.currentTenantId()
                )
                .map(this::toRentabiliteReference);
    }

    @Override
    public FinanceDashboardResponse getDashboardSummary() {
        UUID tenantId = tenantContext.currentTenantId();

        // 1. Calculate Live Rentability for Current Month
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate today = LocalDate.now();
        
        BigDecimal currentMonthMilk = productionQueries.getTotalMilkProductionKg(
                startOfMonth.atStartOfDay().atOffset(java.time.ZoneOffset.UTC),
                today.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC)
        );

        CalculRentabilite currentMonthCalcul = null;
        if (currentMonthMilk != null && currentMonthMilk.signum() > 0) {
            try {
                currentMonthCalcul = rentabiliteService.calculateLive(
                    new CalculateRentabilite(startOfMonth, today)
                );
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Aucun prix de vente")) {
                    throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "MISSING_MILK_PRICE"
                    );
                }
                throw e;
            }
        }

        BigDecimal sumRecentCharges = chargeRepository.findAllByTenantIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDesc(
                tenantId, startOfMonth, today.plusDays(1))
                .stream()
                .map(ChargeExploitation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Kpis kpis = currentMonthCalcul != null ? new Kpis(
                currentMonthCalcul.getCoutRevientParLitre(),
                0, // Trend simplified
                currentMonthCalcul.getVolumeLait().signum() > 0 ? 
                    currentMonthCalcul.getChiffreAffaires().divide(currentMonthCalcul.getVolumeLait(), 4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO,
                currentMonthCalcul.getChiffreAffaires().subtract(currentMonthCalcul.getCoutTotal()),
                0, // Trend simplified
                currentMonthCalcul.getChiffreAffaires(),
                0 // Trend simplified
        ) : new Kpis(
                BigDecimal.ZERO,
                0,
                BigDecimal.ZERO,
                sumRecentCharges.negate(),
                0,
                BigDecimal.ZERO,
                0
        );

        BigDecimal marge = currentMonthCalcul != null ? currentMonthCalcul.getMarge() : sumRecentCharges.negate();
        BigDecimal netMargin = (currentMonthCalcul != null && currentMonthCalcul.getChiffreAffaires().signum() > 0) ?
            currentMonthCalcul.getMarge().divide(currentMonthCalcul.getChiffreAffaires(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
            BigDecimal.ZERO;
            
        Rentability rentability = new Rentability(marge, netMargin, 0);

        // 2. Fetch Recent Charges for this month
        List<ChargeExploitation> recentCharges = chargeRepository.findAllByTenantIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDesc(
                tenantId, startOfMonth, today.plusDays(1));

        List<TransactionResponse> transactions = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (ChargeExploitation c : recentCharges) {
            transactions.add(new TransactionResponse(
                    c.getId().toString(), "CHARGE", c.getCategorie(), c.getMontant().doubleValue(),
                    c.getDate().format(fmt), "COMPLETED", c.getLibelle()
            ));
        }
        
        // 3. Historical Data for Charts
        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<BigDecimal> charges = new ArrayList<>();
        
        List<CalculRentabilite> allSnapshots = calculRentabiliteRepository.findAllByTenantIdOrderByDateCalculDesc(tenantId);
        
        for (int i = 3; i >= 1; i--) {
            YearMonth pastMonth = currentMonth.minusMonths(i);
            labels.add("Mois-" + i);
            
            // Try to find snapshot for that month
            Optional<CalculRentabilite> snapshot = allSnapshots.stream()
                    .filter(c -> YearMonth.from(c.getPeriodeDebut()).equals(pastMonth))
                    .findFirst();
                    
            revenues.add(snapshot.map(CalculRentabilite::getChiffreAffaires).orElse(BigDecimal.ZERO));
            charges.add(snapshot.map(CalculRentabilite::getCoutTotal).orElse(BigDecimal.ZERO));
        }
        
        labels.add("Ce mois");
        revenues.add(currentMonthCalcul != null ? currentMonthCalcul.getChiffreAffaires() : BigDecimal.ZERO);
        charges.add(currentMonthCalcul != null ? currentMonthCalcul.getCoutTotal() : sumRecentCharges);

        RevenueVsCharges charts = new RevenueVsCharges(labels, revenues, charges);

        // 4. Repartition (Alimentation vs Autres Categories)
        BigDecimal alimCost = currentMonthCalcul != null ? currentMonthCalcul.getCoutAlimentation() : BigDecimal.ZERO;
        BigDecimal otherCost = currentMonthCalcul != null ? currentMonthCalcul.getAutresCharges() : sumRecentCharges;
        
        // Simple 2-category split for now, real categorization would group charges by 'categorie'
        BigDecimal totalCost = currentMonthCalcul != null ? currentMonthCalcul.getCoutTotal() : sumRecentCharges;
        int totalCents = totalCost.multiply(BigDecimal.valueOf(100)).intValue();
        int alimPct = totalCents > 0 ? alimCost.multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalCents), java.math.RoundingMode.HALF_UP).intValue() : 0;
        int otherPct = totalCents > 0 ? 100 - alimPct : 0;
        
        ChargesRepartition rep = new ChargesRepartition(
                List.of("Alimentation", "Autres"),
                List.of(alimPct, otherPct),
                List.of("#C87533", "#374151")
        );

        return new FinanceDashboardResponse(kpis, charts, rep, rentability, transactions);
    }
    private AnimalRentabilityReference toAnimalReference(
            AnimalRentabilityResult result
    ) {
        return new AnimalRentabilityReference(
                result.animalId(),
                result.dateDebut(),
                result.dateFin(),
                result.volumeLaitKg(),
                result.volumeLaitLitres(),
                result.chiffreAffaires(),
                result.coutAlimentation(),
                result.marge(),
                result.prixMoyenParLitre(),
                result.coutAlimentationParLitre(),
                result.rentable()
        );
    }

    private RentabiliteReference toRentabiliteReference(
            CalculRentabilite calcul
    ) {
        return new RentabiliteReference(
                calcul.getId(),
                calcul.getPeriodeDebut(),
                calcul.getPeriodeFin(),
                calcul.getDateCalcul(),
                calcul.getVolumeLait(),
                calcul.getChiffreAffaires(),
                calcul.getCoutAlimentation(),
                calcul.getAutresCharges(),
                calcul.getCoutTotal(),
                calcul.getCoutRevientParLitre(),
                calcul.getMarge()
        );
    }
}