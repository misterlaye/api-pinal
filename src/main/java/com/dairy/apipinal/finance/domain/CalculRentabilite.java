package com.dairy.apipinal.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "calcul_rentabilite")
public class CalculRentabilite {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING =
            RoundingMode.HALF_UP;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "date_calcul", nullable = false)
    private OffsetDateTime dateCalcul;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "volume_lait", nullable = false, precision = 19, scale = 4)
    private BigDecimal volumeLait;

    @Column(name = "chiffre_affaires", nullable = false, precision = 19, scale = 4)
    private BigDecimal chiffreAffaires;

    @Column(name = "cout_alimentation", nullable = false, precision = 19, scale = 4)
    private BigDecimal coutAlimentation;

    @Column(name = "autres_charges", nullable = false, precision = 19, scale = 4)
    private BigDecimal autresCharges;

    @Column(name = "cout_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal coutTotal;

    @Column(name = "cout_revient_par_litre", nullable = false, precision = 19, scale = 4)
    private BigDecimal coutRevientParLitre;

    @Column(name = "marge", nullable = false, precision = 19, scale = 4)
    private BigDecimal marge;

    @Version
    @Column(nullable = false)
    private long version;

    protected CalculRentabilite() {
    }

    private CalculRentabilite(
            UUID tenantId,
            OffsetDateTime dateCalcul,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            BigDecimal volumeLait,
            BigDecimal chiffreAffaires,
            BigDecimal coutAlimentation,
            BigDecimal autresCharges,
            BigDecimal coutTotal,
            BigDecimal coutRevientParLitre,
            BigDecimal marge
    ) {
        this.tenantId = tenantId;
        this.dateCalcul = dateCalcul;
        this.periodeDebut = periodeDebut;
        this.periodeFin = periodeFin;
        this.volumeLait = volumeLait;
        this.chiffreAffaires = chiffreAffaires;
        this.coutAlimentation = coutAlimentation;
        this.autresCharges = autresCharges;
        this.coutTotal = coutTotal;
        this.coutRevientParLitre = coutRevientParLitre;
        this.marge = marge;
    }

    public static CalculRentabilite calculate(
            UUID tenantId,
            OffsetDateTime dateCalcul,
            LocalDate periodeDebut,
            LocalDate periodeFin,
            BigDecimal volumeLait,
            BigDecimal chiffreAffaires,
            BigDecimal coutAlimentation,
            BigDecimal autresCharges
    ) {
        Objects.requireNonNull(tenantId);
        Objects.requireNonNull(dateCalcul);
        Objects.requireNonNull(periodeDebut);
        Objects.requireNonNull(periodeFin);

        if (periodeFin.isBefore(periodeDebut)) {
            throw new IllegalArgumentException(
                    "La période de calcul est invalide."
            );
        }

        requireNonNegative(
                volumeLait,
                "Le volume de lait"
        );

        if (volumeLait.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Le volume de lait doit être strictement positif."
            );
        }

        requireNonNegative(
                chiffreAffaires,
                "Le chiffre d'affaires"
        );

        requireNonNegative(
                coutAlimentation,
                "Le coût d'alimentation"
        );

        requireNonNegative(
                autresCharges,
                "Les autres charges"
        );

        BigDecimal coutTotal =
                money(coutAlimentation.add(autresCharges));

        BigDecimal coutRevientParLitre =
                coutTotal.divide(
                        volumeLait,
                        SCALE,
                        ROUNDING
                );

        BigDecimal marge =
                money(chiffreAffaires.subtract(coutTotal));

        return new CalculRentabilite(
                tenantId,
                dateCalcul,
                periodeDebut,
                periodeFin,
                quantity(volumeLait),
                money(chiffreAffaires),
                money(coutAlimentation),
                money(autresCharges),
                coutTotal,
                money(coutRevientParLitre),
                marge
        );
    }

    private static void requireNonNegative(
            BigDecimal value,
            String label
    ) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    label + " doit être supérieur ou égal à zéro."
            );
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    private static BigDecimal quantity(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public OffsetDateTime getDateCalcul() {
        return dateCalcul;
    }

    public LocalDate getPeriodeDebut() {
        return periodeDebut;
    }

    public LocalDate getPeriodeFin() {
        return periodeFin;
    }

    public BigDecimal getVolumeLait() {
        return volumeLait;
    }

    public BigDecimal getChiffreAffaires() {
        return chiffreAffaires;
    }

    public BigDecimal getCoutAlimentation() {
        return coutAlimentation;
    }

    public BigDecimal getAutresCharges() {
        return autresCharges;
    }

    public BigDecimal getCoutTotal() {
        return coutTotal;
    }

    public BigDecimal getCoutRevientParLitre() {
        return coutRevientParLitre;
    }

    public BigDecimal getMarge() {
        return marge;
    }
}