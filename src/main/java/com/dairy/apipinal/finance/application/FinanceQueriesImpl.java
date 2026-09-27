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

@Service
@Transactional(readOnly = true)
public class FinanceQueriesImpl implements FinanceQueries {

    private final AnimalRentabilityService animalRentabilityService;
    private final CalculRentabiliteRepository calculRentabiliteRepository;
    private final ChargeExploitationRepository chargeRepository;
    private final TenantContext tenantContext;

    public FinanceQueriesImpl(
            AnimalRentabilityService animalRentabilityService,
            CalculRentabiliteRepository calculRentabiliteRepository,
            ChargeExploitationRepository chargeRepository,
            TenantContext tenantContext
    ) {
        this.animalRentabilityService = animalRentabilityService;
        this.calculRentabiliteRepository = calculRentabiliteRepository;
        this.chargeRepository = chargeRepository;
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
        Optional<CalculRentabilite> latest = calculRentabiliteRepository.findFirstByTenantIdOrderByDateCalculDesc(tenantId);

        Kpis kpis;
        Rentability rentability;
        List<TransactionResponse> transactions = new ArrayList<>();

        // Fetch recent charges (e.g. last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<ChargeExploitation> recentCharges = chargeRepository.findAllByTenantIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDesc(
                tenantId, thirtyDaysAgo, LocalDate.now().plusDays(1));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (ChargeExploitation c : recentCharges) {
            transactions.add(new TransactionResponse(
                    c.getId().toString(), "CHARGE", c.getCategorie(), c.getMontant().doubleValue(),
                    c.getDate().format(fmt), "COMPLETED", c.getLibelle()
            ));
        }

        if (latest.isPresent()) {
            CalculRentabilite c = latest.get();
            kpis = new Kpis(
                    c.getCoutRevientParLitre(), 0,
                    BigDecimal.valueOf(600), // Defaulting prix moyen as it's not directly in CalculRentabilite
                    c.getChiffreAffaires().subtract(c.getCoutTotal()), 0, // Marge brute simplified
                    c.getChiffreAffaires(), 0
            );

            BigDecimal netMargin = c.getChiffreAffaires().signum() > 0 ?
                c.getMarge().divide(c.getChiffreAffaires(), 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

            rentability = new Rentability(c.getMarge(), netMargin, 0);

            // Add revenue transaction for the rentability period
            transactions.add(0, new TransactionResponse(
                    UUID.randomUUID().toString(), "REVENU", "Vente de lait", c.getChiffreAffaires().doubleValue(),
                    c.getPeriodeFin().format(fmt), "COMPLETED", "Vente Lait - Période " + c.getPeriodeDebut().format(fmt) + " au " + c.getPeriodeFin().format(fmt)
            ));
        } else {
            kpis = new Kpis(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
            rentability = new Rentability(BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        RevenueVsCharges charts = new RevenueVsCharges(
                List.of("Mois-3", "Mois-2", "Mois-1", "Ce mois"),
                List.of(BigDecimal.valueOf(120000), BigDecimal.valueOf(150000), BigDecimal.valueOf(130000), latest.map(CalculRentabilite::getChiffreAffaires).orElse(BigDecimal.ZERO)),
                List.of(BigDecimal.valueOf(80000), BigDecimal.valueOf(95000), BigDecimal.valueOf(85000), latest.map(CalculRentabilite::getCoutTotal).orElse(BigDecimal.ZERO))
        );

        ChargesRepartition rep = new ChargesRepartition(
                List.of("Alimentation", "Santé", "Main d'oeuvre"),
                List.of(60, 25, 15),
                List.of("#C87533", "#EF4444", "#374151")
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