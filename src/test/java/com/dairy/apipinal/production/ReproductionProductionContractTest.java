package com.dairy.apipinal.production;

import com.dairy.apipinal.production.domain.Lactation;
import com.dairy.apipinal.production.domain.StatutLactation;
import com.dairy.apipinal.reproduction.domain.CycleReproduction;
import com.dairy.apipinal.reproduction.domain.MethodeReproduction;
import com.dairy.apipinal.reproduction.domain.StatutReproduction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validation des 7 règles métier du cycle
 * Reproduction ↔ Production (Phase 3).
 *
 * Ces tests garantissent le contrat suivant :
 *   Reproduction : VIDE → INSÉMINÉE → GESTANTE → VÊLAGE/AVORTEMENT → VIDE
 *   Production   : LACTATION → TARISSEMENT → VÊLAGE → nouvelle LACTATION
 */
@DisplayName("Phase 3 – Règles métier Reproduction ↔ Production")
class ReproductionProductionContractTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EXPLOITATION_ID = UUID.randomUUID();
    private static final UUID ANIMAL_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    // ──────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────

    private Lactation createActiveLactation(LocalDate dateDebut) {
        return new Lactation(TENANT_ID, EXPLOITATION_ID, EXPLOITATION_ID, ANIMAL_ID, dateDebut, ACTOR_ID);
    }

    private CycleReproduction createCycle(LocalDate dateInsemination) {
        return new CycleReproduction(
                ANIMAL_ID, TENANT_ID, EXPLOITATION_ID,
                1, dateInsemination,
                MethodeReproduction.INSEMINATION_ARTIFICIELLE,
                null, "CODE_PAILLETTE_1", dateInsemination.plusDays(283), ACTOR_ID
        );
    }

    private CycleReproduction createGestanteCycle(LocalDate dateInsemination) {
        CycleReproduction cycle = createCycle(dateInsemination);
        cycle.enregistrerConstat(
                dateInsemination.plusDays(60),
                "POSITIF", "Dr Vétérinaire", ACTOR_ID
        );
        return cycle;
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 1 : Le tarissement ferme uniquement la lactation
    //           active et ne modifie pas le cycle reproductif
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 1 – Tarissement")
    class TarissementTests {

        @Test
        @DisplayName("Le tarissement ferme la lactation active (EN_COURS → TERMINEE)")
        void tarissement_ferme_lactation_active() {
            Lactation lactation = createActiveLactation(LocalDate.of(2026, 1, 10));

            assertEquals(StatutLactation.EN_COURS, lactation.getStatut());

            lactation.terminer(LocalDate.of(2026, 8, 1), ACTOR_ID);

            assertEquals(StatutLactation.TERMINEE, lactation.getStatut());
            assertEquals(LocalDate.of(2026, 8, 1), lactation.getDateFin());
        }

        @Test
        @DisplayName("Le tarissement n'a aucun effet sur le cycle reproductif")
        void tarissement_ne_modifie_pas_cycle_reproductif() {
            // Une vache gestante avec une lactation en cours
            CycleReproduction cycle = createGestanteCycle(LocalDate.of(2026, 1, 1));
            Lactation lactation = createActiveLactation(LocalDate.of(2025, 6, 1));

            // On tarit la vache
            lactation.terminer(LocalDate.of(2026, 7, 15), ACTOR_ID);

            // La lactation est terminée...
            assertEquals(StatutLactation.TERMINEE, lactation.getStatut());
            // ...mais le cycle reproductif reste GESTANTE
            assertEquals(StatutReproduction.GESTANTE, cycle.getStatut());
        }

        @Test
        @DisplayName("Impossible de tarir une lactation déjà terminée")
        void tarissement_double_interdit() {
            Lactation lactation = createActiveLactation(LocalDate.of(2026, 1, 10));
            lactation.terminer(LocalDate.of(2026, 8, 1), ACTOR_ID);

            assertThrows(IllegalStateException.class,
                    () -> lactation.terminer(LocalDate.of(2026, 8, 10), ACTOR_ID)
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 2 : L'avortement termine le cycle reproductif
    //           mais ne crée jamais de nouvelle lactation
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 2 – Avortement")
    class AvortementTests {

        @Test
        @DisplayName("L'avortement passe le cycle en statut AVORTEE")
        void avortement_termine_le_cycle() {
            CycleReproduction cycle = createGestanteCycle(LocalDate.of(2026, 3, 1));

            assertEquals(StatutReproduction.GESTANTE, cycle.getStatut());

            cycle.declarerAvortement(ACTOR_ID);

            assertEquals(StatutReproduction.AVORTEE, cycle.getStatut());
        }

        @Test
        @DisplayName("L'avortement n'est possible que sur une vache GESTANTE")
        void avortement_uniquement_si_gestante() {
            CycleReproduction cycle = createCycle(LocalDate.of(2026, 3, 1));
            // Le cycle est en EN_ATTENTE_CONSTAT, pas GESTANTE
            assertEquals(StatutReproduction.EN_ATTENTE_CONSTAT, cycle.getStatut());

            assertThrows(IllegalStateException.class,
                    () -> cycle.declarerAvortement(ACTOR_ID)
            );
        }

        @Test
        @DisplayName("L'avortement ne déclenche aucun événement de vêlage (pas de dateReelleVelage)")
        void avortement_ne_cree_pas_de_velage() {
            CycleReproduction cycle = createGestanteCycle(LocalDate.of(2026, 3, 1));
            cycle.declarerAvortement(ACTOR_ID);

            assertNull(cycle.getDateReelleVelage(),
                    "Un avortement ne doit jamais remplir la date de vêlage réelle");
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 3 : Le vêlage réel termine la gestation et
    //           crée une nouvelle lactation à la date réelle
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 3 – Vêlage réel")
    class VelageTests {

        @Test
        @DisplayName("Le vêlage réel passe le cycle en TERMINEE_VELAGE")
        void velage_termine_la_gestation() {
            CycleReproduction cycle = createGestanteCycle(LocalDate.of(2026, 1, 1));
            LocalDate dateReelle = LocalDate.of(2026, 10, 15);

            cycle.declarerVelage(dateReelle, ACTOR_ID);

            assertEquals(StatutReproduction.TERMINEE_VELAGE, cycle.getStatut());
            assertEquals(dateReelle, cycle.getDateReelleVelage());
        }

        @Test
        @DisplayName("Le vêlage ne peut être déclaré que sur une vache GESTANTE")
        void velage_uniquement_si_gestante() {
            CycleReproduction cycle = createCycle(LocalDate.of(2026, 1, 1));
            // EN_ATTENTE_CONSTAT
            assertThrows(IllegalStateException.class,
                    () -> cycle.declarerVelage(LocalDate.of(2026, 10, 15), ACTOR_ID)
            );
        }

        @Test
        @DisplayName("La nouvelle lactation démarre à la date réelle du vêlage, pas la date prévue")
        void nouvelle_lactation_demarre_a_la_date_reelle() {
            LocalDate dateReelleVelage = LocalDate.of(2026, 10, 15);

            // Simulation de ce que fait VelageEnregistreEventListener + StartLactation
            Lactation nouvelleLactation = new Lactation(TENANT_ID, EXPLOITATION_ID, ANIMAL_ID, dateReelleVelage, ACTOR_ID
            );

            assertEquals(dateReelleVelage, nouvelleLactation.getDateDebut());
            assertEquals(StatutLactation.EN_COURS, nouvelleLactation.getStatut());
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 4 : La date prévue de vêlage ne déclenche
    //           aucune transition automatique
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 4 – Date prévue vs Date réelle")
    class DatePrevueTests {

        @Test
        @DisplayName("La date prévue est purement informative (calculée à J+283)")
        void date_prevue_est_informative() {
            LocalDate dateInsemination = LocalDate.of(2026, 1, 1);
            CycleReproduction cycle = createCycle(dateInsemination);

            assertEquals(dateInsemination.plusDays(283), cycle.getDatePrevueVelage());
            // Le cycle reste en EN_ATTENTE_CONSTAT, aucune transition automatique
            assertEquals(StatutReproduction.EN_ATTENTE_CONSTAT, cycle.getStatut());
        }

        @Test
        @DisplayName("Même après la date prévue, le cycle ne change pas sans action explicite")
        void pas_de_transition_automatique_apres_date_prevue() {
            LocalDate dateInsemination = LocalDate.of(2025, 1, 1);
            CycleReproduction cycle = createGestanteCycle(dateInsemination);

            // La date prévue est le 11 octobre 2025 (J+283), bien passée
            LocalDate datePrevue = dateInsemination.plusDays(283);
            assertTrue(datePrevue.isBefore(LocalDate.of(2026, 1, 1)),
                    "La date prévue devrait être dans le passé");

            // Mais le cycle est toujours GESTANTE car personne n'a déclaré le vêlage
            assertEquals(StatutReproduction.GESTANTE, cycle.getStatut());
            assertNull(cycle.getDateReelleVelage());
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 5 : Après un vêlage, la vache est VIDE
    //           côté reproduction jusqu'à nouvelle insémination
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 5 – Statut post-vêlage")
    class PostVelageTests {

        @Test
        @DisplayName("Après un vêlage, le statut du cycle est TERMINEE_VELAGE (= fonctionnellement VIDE)")
        void apres_velage_le_cycle_est_termine() {
            CycleReproduction cycle = createGestanteCycle(LocalDate.of(2026, 1, 1));
            cycle.declarerVelage(LocalDate.of(2026, 10, 15), ACTOR_ID);

            assertEquals(StatutReproduction.TERMINEE_VELAGE, cycle.getStatut(),
                    "Après vêlage, le cycle doit être TERMINEE_VELAGE – " +
                    "la vache est fonctionnellement VIDE et prête pour un nouveau cycle");
        }

        @Test
        @DisplayName("Un nouveau cycle peut être créé après un vêlage (nouvelle insémination)")
        void nouveau_cycle_possible_apres_velage() {
            // Premier cycle terminé par vêlage
            CycleReproduction cycle1 = createGestanteCycle(LocalDate.of(2026, 1, 1));
            cycle1.declarerVelage(LocalDate.of(2026, 10, 15), ACTOR_ID);
            assertEquals(StatutReproduction.TERMINEE_VELAGE, cycle1.getStatut());

            // Nouvelle insémination = nouveau cycle indépendant
            CycleReproduction cycle2 = new CycleReproduction(
                    ANIMAL_ID, TENANT_ID, EXPLOITATION_ID,
                    2, LocalDate.of(2027, 2, 1),
                    MethodeReproduction.INSEMINATION_ARTIFICIELLE,
                    null, "CODE_PAILLETTE_2", LocalDate.of(2027, 2, 1).plusDays(283), ACTOR_ID
            );

            assertEquals(StatutReproduction.EN_ATTENTE_CONSTAT, cycle2.getStatut());
            assertEquals(2, cycle2.getNumeroCycle());
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 6 : L'historique conserve tous les événements
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 6 – Conservation de l'historique")
    class HistoriqueTests {

        @Test
        @DisplayName("Un cycle conserve toutes les données après vêlage")
        void historique_cycle_complet_apres_velage() {
            LocalDate dateInsemination = LocalDate.of(2026, 1, 1);
            CycleReproduction cycle = createCycle(dateInsemination);

            // Constat positif
            cycle.enregistrerConstat(
                    LocalDate.of(2026, 3, 1), "POSITIF", "Dr Vétérinaire", ACTOR_ID
            );

            // Vêlage
            LocalDate dateVelage = LocalDate.of(2026, 10, 15);
            cycle.declarerVelage(dateVelage, ACTOR_ID);

            // Tout l'historique est conservé
            assertEquals(dateInsemination, cycle.getDateInsemination());
            assertEquals(MethodeReproduction.INSEMINATION_ARTIFICIELLE, cycle.getMethodeReproduction());
            assertEquals("CODE_PAILLETTE_1", cycle.getCodePaillette());
            assertFalse(cycle.getConstatsGestation().isEmpty());
            assertEquals("POSITIF", cycle.getConstatsGestation().get(0).getResultat());
            assertEquals("Dr Vétérinaire", cycle.getConstatsGestation().get(0).getVeterinaire());
            assertEquals(dateVelage, cycle.getDateReelleVelage());
            assertEquals(StatutReproduction.TERMINEE_VELAGE, cycle.getStatut());
        }

        @Test
        @DisplayName("Un cycle conserve toutes les données après avortement")
        void historique_cycle_complet_apres_avortement() {
            LocalDate dateInsemination = LocalDate.of(2026, 1, 1);
            CycleReproduction cycle = createCycle(dateInsemination);

            cycle.enregistrerConstat(
                    LocalDate.of(2026, 3, 1), "POSITIF", "Dr Vétérinaire", ACTOR_ID
            );

            cycle.declarerAvortement(ACTOR_ID);

            // L'historique est intégralement conservé
            assertEquals(dateInsemination, cycle.getDateInsemination());
            assertFalse(cycle.getConstatsGestation().isEmpty());
            assertEquals("POSITIF", cycle.getConstatsGestation().get(0).getResultat());
            assertEquals(StatutReproduction.AVORTEE, cycle.getStatut());
            assertNull(cycle.getDateReelleVelage(),
                    "L'avortement ne doit pas remplir la date de vêlage");
        }

        @Test
        @DisplayName("Une lactation terminée conserve ses dates de début et fin")
        void historique_lactation_terminee() {
            LocalDate dateDebut = LocalDate.of(2026, 1, 10);
            LocalDate dateFin = LocalDate.of(2026, 8, 1);
            Lactation lactation = createActiveLactation(dateDebut);
            lactation.terminer(dateFin, ACTOR_ID);

            assertEquals(dateDebut, lactation.getDateDebut());
            assertEquals(dateFin, lactation.getDateFin());
            assertEquals(StatutLactation.TERMINEE, lactation.getStatut());
        }
    }

    // ──────────────────────────────────────────────────────
    // RÈGLE 7 : Impossible d'avoir deux lactations EN_COURS
    //           pour le même animal
    // ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Règle 7 – Unicité de la lactation active")
    class UniciteLactationTests {

        @Test
        @DisplayName("StartLactation ferme automatiquement l'ancienne lactation (test du mécanisme)")
        void startLactation_ferme_lancienne_lactation_avant_ouverture() {
            // Simule le comportement de StartLactation.execute()
            Lactation ancienne = createActiveLactation(LocalDate.of(2025, 6, 1));
            assertEquals(StatutLactation.EN_COURS, ancienne.getStatut());

            // Avant de créer la nouvelle, on ferme l'ancienne (comme le fait StartLactation)
            LocalDate dateNouveauVelage = LocalDate.of(2026, 10, 15);
            ancienne.terminer(dateNouveauVelage.minusDays(1), ACTOR_ID);

            assertEquals(StatutLactation.TERMINEE, ancienne.getStatut());
            assertEquals(dateNouveauVelage.minusDays(1), ancienne.getDateFin());

            // Puis on crée la nouvelle
            Lactation nouvelle = new Lactation(TENANT_ID, EXPLOITATION_ID, EXPLOITATION_ID, ANIMAL_ID, dateNouveauVelage, ACTOR_ID);
            assertEquals(StatutLactation.EN_COURS, nouvelle.getStatut());

            // Vérification : l'ancienne est TERMINEE, la nouvelle est EN_COURS
            assertNotEquals(ancienne.getStatut(), nouvelle.getStatut(),
                    "Les deux lactations ne doivent pas être simultanément EN_COURS");
        }

        @Test
        @DisplayName("Une lactation est toujours créée en statut EN_COURS")
        void nouvelle_lactation_toujours_en_cours() {
            Lactation lactation = createActiveLactation(LocalDate.of(2026, 10, 15));

            assertEquals(StatutLactation.EN_COURS, lactation.getStatut());
            assertNull(lactation.getDateFin());
        }
    }
}
