package com.dairy.apipinal.finance.application;

import com.dairy.apipinal.finance.api.ResultatFinancier;
import com.dairy.apipinal.finance.domain.CalculRentabilite;
import com.dairy.apipinal.finance.infrastructure.persistence.CalculRentabiliteRepository;
import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Snapshot de rentabilité d'une période, construit à partir du calcul financier de référence
 * ({@link PeriodeFinanciereService}). Un snapshot n'est produit que si toutes les données sont connues.
 */
@Service
@Transactional
public class RentabiliteService {

    private final PeriodeFinanciereService periodeFinanciereService;
    private final CalculRentabiliteRepository calculRepository;
    private final TenantContext tenantContext;

    public RentabiliteService(
            PeriodeFinanciereService periodeFinanciereService,
            CalculRentabiliteRepository calculRepository,
            TenantContext tenantContext
    ) {
        this.periodeFinanciereService = periodeFinanciereService;
        this.calculRepository = calculRepository;
        this.tenantContext = tenantContext;
    }

    public CalculRentabilite calculate(CalculateRentabilite command) {
        return calculRepository.save(calculateLive(command));
    }

    /**
     * @throws IllegalStateException si une donnée nécessaire manque (prix du lait, prix d'un aliment, aucune traite).
     */
    public CalculRentabilite calculateLive(CalculateRentabilite command) {
        if (command.periodeDebut() == null || command.periodeFin() == null) {
            throw new IllegalArgumentException("Les bornes de la période sont obligatoires.");
        }

        ResultatFinancier resultat = periodeFinanciereService.calculer(command.periodeDebut(), command.periodeFin());

        if (!resultat.complet() || resultat.volumeLitres().signum() == 0) {
            throw new IllegalStateException("Rentabilité non calculable : " + resultat.avertissements().stream()
                    .map(ResultatFinancier.Avertissement::message)
                    .collect(Collectors.joining(" ")));
        }

        return CalculRentabilite.calculate(
                tenantContext.currentTenantId(),
                tenantContext.currentExploitationId(),
                OffsetDateTime.now(ZoneOffset.UTC),
                command.periodeDebut(),
                command.periodeFin(),
                resultat.volumeLitres(),
                resultat.chiffreAffaires(),
                resultat.coutAlimentation(),
                resultat.autresCharges()
        );
    }
}
