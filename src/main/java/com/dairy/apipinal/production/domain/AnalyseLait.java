package com.dairy.apipinal.production.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyse_lait")
public class AnalyseLait {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "exploitation_id", nullable = false)
    private UUID exploitationId;

    @Column(name = "lactation_id", nullable = false)
    private UUID lactationId;

    @Column(name = "auteur_id", nullable = false)
    private UUID auteurId;

    @Column(name = "date_analyse", nullable = false)
    private LocalDate dateAnalyse;

    @Column(name = "taux_butyreux", precision = 5, scale = 2)
    private BigDecimal tauxButyreux;

    @Column(name = "taux_proteique", precision = 5, scale = 2)
    private BigDecimal tauxProteique;

    @Column(name = "cellules_somatiques")
    private Integer cellulesSomatiques;

    @Column(name = "date_heure_saisie", nullable = false)
    private OffsetDateTime dateHeureSaisie;

    @Version
    @Column(nullable = false)
    private long version;

    protected AnalyseLait() {
    }

    public AnalyseLait(
            UUID tenantId,
            UUID exploitationId,
            UUID lactationId,
            UUID auteurId,
            LocalDate dateAnalyse,
            BigDecimal tauxButyreux,
            BigDecimal tauxProteique,
            Integer cellulesSomatiques,
            OffsetDateTime dateHeureSaisie
    ) {
        if (dateAnalyse == null) {
            throw new IllegalArgumentException("La date de l'analyse est obligatoire.");
        }
        if (tauxButyreux != null && tauxButyreux.signum() < 0) {
            throw new IllegalArgumentException("Le taux butyreux ne peut pas être négatif.");
        }
        if (tauxProteique != null && tauxProteique.signum() < 0) {
            throw new IllegalArgumentException("Le taux protéique ne peut pas être négatif.");
        }
        if (cellulesSomatiques != null && cellulesSomatiques < 0) {
            throw new IllegalArgumentException("Le nombre de cellules somatiques ne peut pas être négatif.");
        }

        this.tenantId = tenantId;
        this.exploitationId = exploitationId;
        this.lactationId = lactationId;
        this.auteurId = auteurId;
        this.dateAnalyse = dateAnalyse;
        this.tauxButyreux = tauxButyreux;
        this.tauxProteique = tauxProteique;
        this.cellulesSomatiques = cellulesSomatiques;
        this.dateHeureSaisie = dateHeureSaisie;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getExploitationId() {
        return exploitationId;
    }

    public UUID getLactationId() {
        return lactationId;
    }

    public UUID getAuteurId() {
        return auteurId;
    }

    public LocalDate getDateAnalyse() {
        return dateAnalyse;
    }

    public BigDecimal getTauxButyreux() {
        return tauxButyreux;
    }

    public BigDecimal getTauxProteique() {
        return tauxProteique;
    }

    public Integer getCellulesSomatiques() {
        return cellulesSomatiques;
    }

    public OffsetDateTime getDateHeureSaisie() {
        return dateHeureSaisie;
    }
}
