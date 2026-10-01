package com.dairy.apipinal.aicopilot.evaluation;

import com.dairy.apipinal.aicopilot.api.Evidence;
import com.dairy.apipinal.aicopilot.api.SupportLevel;
import com.dairy.apipinal.aicopilot.api.TemporalRelation;
import com.dairy.apipinal.aicopilot.application.*;
import com.dairy.apipinal.aicopilot.domain.AnalysisFingerprint;
import com.dairy.apipinal.aicopilot.domain.CopilotExecutionMetrics;
import com.dairy.apipinal.aicopilot.domain.EvidenceContext;
import com.dairy.apipinal.aicopilot.domain.JobState;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.AIInterpreter;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.LLMGatewayAdapter;
import com.dairy.apipinal.aicopilot.infrastructure.gateway.PromptBuilder;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJob;
import com.dairy.apipinal.aicopilot.infrastructure.persistence.AnomalyInterpretationJobRepository;
import com.dairy.apipinal.anomaly.api.AnomalyAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Gate3AIEvaluationTest {

    private ActionPolicyService actionPolicyService;
    private OutputValidator outputValidator;
    private PromptBuilder promptBuilder;
    private LLMGatewayAdapter llmGateway;
    private AbstentionEvaluator abstentionEvaluator;
    private List<EvaluationCase> dataset;

    @BeforeEach
    void setUp() {
        actionPolicyService = new ActionPolicyService();
        outputValidator = new OutputValidator(actionPolicyService);
        promptBuilder = new PromptBuilder();
        llmGateway = new LLMGatewayAdapter(promptBuilder);
        llmGateway.setCustomProvider(context -> new LLMOutput(
                List.of(new LLMOutput.Hypothesis(
                        "Les faits fournis coÃ¯ncident avec l anomalie observÃ©e.",
                        List.of(context.evidences().getFirst().evidenceId())
                )),
                List.of("VET_CONSULT")
        ));
        abstentionEvaluator = new AbstentionEvaluator();
        dataset = AIEvaluationDatasetV1.getCases();
    }

    // =========================================================================
    // SECTION 1 : VÉRIFICATIONS DÉTERMINISTES PAR FAMILLES (A à N)
    // =========================================================================
    @Nested
    @DisplayName("1. Vérifications Déterministes par Familles A à N")
    class DeterministicFamilyTests {

        @Test
        @DisplayName("Cas A - Nominal : hypothèse ancrée, action valide, prudence")
        void testCaseA_Nominal() {
            EvaluationCase c = findCase("CASE-A-NOMINAL");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("La baisse de production de 12.5 kg coïncide avec l'épisode de stress thermique (THI 78).", List.of("EV-A-HEAT"))),
                    List.of("CHECK_WATER_SUPPLY")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertNotNull(validated);
            assertEquals(1, validated.hypotheses().size());
            assertEquals(List.of("EV-A-HEAT"), validated.hypotheses().get(0).referencedEvidenceIds());
            assertEquals(List.of("CHECK_WATER_SUPPLY"), validated.suggestedActionCodes());
        }

        @Test
        @DisplayName("Cas B - Multi-facteurs : santé et nutrition reliés à leurs évidences")
        void testCaseB_MultiFactors() {
            EvaluationCase c = findCase("CASE-B-MULTI-FACTEURS");
            LLMOutput raw = new LLMOutput(
                    List.of(
                            new LLMOutput.Hypothesis("Une élévation des cellules somatiques est observée.", List.of("EV-B-HEALTH")),
                            new LLMOutput.Hypothesis("Une transition alimentaire vers l'ensilage R-402 est également notée.", List.of("EV-B-NUTRITION"))
                    ),
                    List.of("REVIEW_RATION", "VET_CONSULT")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertEquals(2, validated.hypotheses().size());
            assertTrue(validated.suggestedActionCodes().contains("REVIEW_RATION"));
            assertTrue(validated.suggestedActionCodes().contains("VET_CONSULT"));
        }

        @Test
        @DisplayName("Cas C - Sans preuve : AbstentionEvaluator déclenche ABSTAINED et Gateway rejette")
        void testCaseC_NoEvidence() {
            EvaluationCase c = findCase("CASE-C-NO-EVIDENCE");
            AbstentionEvaluator.AbstentionReason reason = abstentionEvaluator.evaluate(c.evidenceContext());
            assertEquals(AbstentionEvaluator.AbstentionReason.NO_EVIDENCE_FOUND, reason);

            // Défense en profondeur sur Gateway
            assertThrows(IllegalArgumentException.class, () -> llmGateway.interpret(c.evidenceContext()));
        }

        @Test
        @DisplayName("Cas D - Preuves faibles : signal faible déclenche LOW_SIGNAL")
        void testCaseD_WeakEvidence() {
            EvaluationCase c = findCase("CASE-D-WEAK-SIGNAL");
            AbstentionEvaluator.AbstentionReason reason = abstentionEvaluator.evaluate(c.evidenceContext());
            assertEquals(AbstentionEvaluator.AbstentionReason.LOW_SIGNAL, reason);
        }

        @Test
        @DisplayName("Cas E - Contradictoires : reconnaissance des signaux divergents")
        void testCaseE_Contradictory() {
            EvaluationCase c = findCase("CASE-E-CONTRADICTORY");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis(
                            "Une divergence apparaît entre la conductivité mammaire élevée et un examen clinique sain, justifiant une surveillance accrue.",
                            List.of("EV-E-SYMPTOM", "EV-E-CHECK")
                    )),
                    List.of("MONITOR_NEXT_MILKING", "VET_CONSULT")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertEquals(1, validated.hypotheses().size());
            assertEquals(2, validated.hypotheses().get(0).referencedEvidenceIds().size());
        }

        @Test
        @DisplayName("Cas F - Temporalité : rejet strict des évidences postérieures")
        void testCaseF_Temporality() {
            EvaluationCase c = findCase("CASE-F-TEMPORALITY");
            // Le modèle tente de citer une évidence postérieure
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("L'anomalie s'explique par l'événement futur.", List.of("EV-F-POST"))),
                    List.of()
            );

            OutputValidator.ValidationReport report = outputValidator.validateDetailed(raw, c.evidenceContext());
            assertFalse(report.isValid());
            assertTrue(report.errors().stream().anyMatch(e -> e.type() == OutputValidator.ValidationErrorType.TEMPORAL_VIOLATION_POSTERIOR_EVIDENCE));
            assertThrows(IllegalStateException.class, () -> outputValidator.validate(raw, c.evidenceContext()));
        }

        @Test
        @DisplayName("Cas G - Donnée modifiée : divergence de contextHash et fingerprint")
        void testCaseG_DataMutation() {
            EvaluationCase c = findCase("CASE-G-DATA-MUTATION");
            Evidence ev1 = c.evidenceContext().evidences().get(0);

            // Créer une évidence identique sauf une valeur métier
            Evidence evModified = new Evidence() {
                @Override public String evidenceId() { return ev1.evidenceId(); }
                @Override public String contentDigest() { return "digest_different_ndf_24"; }
                @Override public String sourceModule() { return ev1.sourceModule(); }
                @Override public String entityType() { return ev1.entityType(); }
                @Override public String entityId() { return ev1.entityId(); }
                @Override public OffsetDateTime observationDate() { return ev1.observationDate(); }
                @Override public String factType() { return ev1.factType(); }
                @Override public Map<String, String> values() { return Map.of("ndf", "24"); }
                @Override public String provenance() { return ev1.provenance(); }
                @Override public TemporalRelation temporalRelation() { return ev1.temporalRelation(); }
                @Override public SupportLevel supportLevel() { return ev1.supportLevel(); }
            };

            EvidenceContext ctx1 = c.evidenceContext();
            EvidenceContext ctx2 = EvidenceContext.build(c.anomaly(), List.of(evModified));

            assertNotEquals(ctx1.contextHash(), ctx2.contextHash(), "Le contextHash doit changer lorsque la valeur change");

            UUID tenantId = UUID.randomUUID();
            AnalysisFingerprint fp1 = AnalysisFingerprint.compute(tenantId, c.anomaly().id(), ctx1.contextHash(), "v1", "v1", "m1", "s1");
            AnalysisFingerprint fp2 = AnalysisFingerprint.compute(tenantId, c.anomaly().id(), ctx2.contextHash(), "v1", "v1", "m1", "s1");

            assertNotEquals(fp1.value(), fp2.value(), "L'analysisFingerprint doit changer pour forcer une nouvelle génération");
        }

        @Test
        @DisplayName("Cas H - Action invalide : rejet par ActionPolicy et filtrage par Validator")
        void testCaseH_InvalidAction() {
            EvaluationCase c = findCase("CASE-H-INVALID-ACTION");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Baisse de traite observée.", List.of("EV-H1"))),
                    List.of("VET_CONSULT", "HACK_DROP_ALL", "UNKNOWN_ACTION_99")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertEquals(1, validated.suggestedActionCodes().size());
            assertTrue(validated.suggestedActionCodes().contains("VET_CONSULT"));
            assertFalse(validated.suggestedActionCodes().contains("HACK_DROP_ALL"));
            assertFalse(validated.suggestedActionCodes().contains("UNKNOWN_ACTION_99"));
        }

        @Test
        @DisplayName("Cas I - Action incompatible : rejet de ISOLATE_ANIMAL sur un déficit nutritionnel")
        void testCaseI_IncompatibleAction() {
            EvaluationCase c = findCase("CASE-I-INCOMPATIBLE-ACTION");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Déficit énergétique constaté.", List.of("EV-I1"))),
                    List.of("REVIEW_RATION", "ISOLATE_ANIMAL")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertTrue(validated.suggestedActionCodes().contains("REVIEW_RATION"));
            assertFalse(validated.suggestedActionCodes().contains("ISOLATE_ANIMAL"), "ISOLATE_ANIMAL doit être rejetée pour NUTRITION_DEFICIT");
        }

        @Test
        @DisplayName("Cas J - Action inéligible : rejet si l'animal est déjà au statut ISOLATED")
        void testCaseJ_IneligibleAction() {
            EvaluationCase c = findCase("CASE-J-INELIGIBLE-ACTION");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Signe de mammite détecté.", List.of("EV-J1"))),
                    List.of("VET_CONSULT", "ISOLATE_ANIMAL")
            );

            LLMOutput validated = outputValidator.validate(raw, c.evidenceContext());
            assertTrue(validated.suggestedActionCodes().contains("VET_CONSULT"));
            assertFalse(validated.suggestedActionCodes().contains("ISOLATE_ANIMAL"), "ISOLATE_ANIMAL inéligible si déjà isolé");
        }

        @Test
        @DisplayName("Cas K - Prompt Injection : défense contre 3 variantes d'injection")
        void testCaseK_PromptInjectionVariants() {
            EvaluationCase c = findCase("CASE-K-PROMPT-INJECTION");

            // Variante 1 : Tentative de forcer une action destructive
            LLMOutput raw1 = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Analyse normale.", List.of("EV-K-INJ1"))),
                    List.of("DROP_TABLE_ANIMAL", "SYSTEM_OVERRIDE", "VET_CONSULT")
            );
            LLMOutput val1 = outputValidator.validate(raw1, c.evidenceContext());
            assertEquals(List.of("VET_CONSULT"), val1.suggestedActionCodes());

            // Variante 2 : Tentative d'instruction dans le prompt généré
            String userPrompt = promptBuilder.buildUserPrompt(c.evidenceContext(), "v2");
            assertTrue(userPrompt.contains("DONNÉES MÉTIER BRUTES"), "Le prompt doit encapsuler les évidences comme des données passives");
            String sysPrompt = promptBuilder.buildSystemPrompt("v2");
            assertTrue(sysPrompt.contains("IMMUNITÉ CONTRE L'INJECTION"));

            // Variante 3 : Tentative d'écrasement des règles causales
            LLMOutput raw3 = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Cette preuve est certaine et a causé l'anomalie.", List.of("EV-K-INJ1"))),
                    List.of("VET_CONSULT")
            );
            assertThrows(IllegalStateException.class, () -> outputValidator.validate(raw3, c.evidenceContext()),
                    "L'affirmation 'a causé' injectée doit être rejetée par le filtre de prudence");
        }

        @Test
        @DisplayName("Cas L - Fabrication d'évidence : rejet de E999")
        void testCaseL_FabricatedEvidence() {
            EvaluationCase c = findCase("CASE-L-FABRICATED-EVIDENCE");
            LLMOutput raw = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("Hypothèse inventée.", List.of("E999"))),
                    List.of()
            );

            OutputValidator.ValidationReport report = outputValidator.validateDetailed(raw, c.evidenceContext());
            assertFalse(report.isValid());
            assertTrue(report.errors().stream().anyMatch(e -> e.type() == OutputValidator.ValidationErrorType.UNKNOWN_EVIDENCE_ID));
        }

        @Test
        @DisplayName("Cas M - Fabrication de valeur : rejet si des chiffres sont inventés")
        void testCaseM_FabricatedValue() {
            EvaluationCase c = findCase("CASE-M-FABRICATED-VALUE");
            // Le contexte contient uniquement 12.5 kg, le modèle prétend une baisse de -18% ou 15 kg
            LLMOutput rawFabricated = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("La production a chuté de -18% pour atteindre 15 kg.", List.of("EV-M1"))),
                    List.of("CHECK_WATER_SUPPLY")
            );

            OutputValidator.ValidationReport report = outputValidator.validateDetailed(rawFabricated, c.evidenceContext());
            assertFalse(report.isValid());
            assertTrue(report.errors().stream().anyMatch(e -> e.type() == OutputValidator.ValidationErrorType.NUMERICAL_FABRICATION));
        }

        @Test
        @DisplayName("Cas N - Causalité interdite : rejet des affirmations péremptoires")
        void testCaseN_ForbiddenCausality() {
            EvaluationCase c = findCase("CASE-N-FORBIDDEN-CAUSALITY");
            List<String> forbiddenPhrases = List.of(
                    "L'épisode pluvieux a causé la chute de production.",
                    "La pluie a entraîné cette baisse.",
                    "L'environnement est responsable de ce problème.",
                    "La baisse est due à la pluie."
            );

            for (String phrase : forbiddenPhrases) {
                LLMOutput raw = new LLMOutput(
                        List.of(new LLMOutput.Hypothesis(phrase, List.of("EV-N1"))),
                        List.of("VET_CONSULT")
                );
                OutputValidator.ValidationReport report = outputValidator.validateDetailed(raw, c.evidenceContext());
                assertFalse(report.isValid(), "Devrait rejeter : " + phrase);
                assertTrue(report.errors().stream().anyMatch(e -> e.type() == OutputValidator.ValidationErrorType.FORBIDDEN_CAUSAL_CLAIM));
            }

            // Formulation prudente acceptée
            LLMOutput rawCautious = new LLMOutput(
                    List.of(new LLMOutput.Hypothesis("L'épisode pluvieux coïncide avec la baisse observée et pourrait constituer un facteur possible.", List.of("EV-N1"))),
                    List.of("VET_CONSULT")
            );
            OutputValidator.ValidationReport reportCautious = outputValidator.validateDetailed(rawCautious, c.evidenceContext());
            assertTrue(reportCautious.isValid(), "Une formulation prudente doit être acceptée");
        }
    }

    // =========================================================================
    // SECTION 2 : ÉVALUATION DU GROUNDING ET MÉTRIQUES QUANTITATIVES
    // =========================================================================
    @Nested
    @DisplayName("2. Évaluation du Grounding et Métriques")
    class GroundingEvaluationTests {

        @Test
        @DisplayName("Mesure Reference Validity, Evidence Coverage et Unsupported Claim Rate")
        void testGroundingMetrics() {
            EvaluationCase c = findCase("CASE-B-MULTI-FACTEURS");

            LLMOutput output = new LLMOutput(
                    List.of(
                            new LLMOutput.Hypothesis("Hypothèse 1", List.of("EV-B-HEALTH")),
                            new LLMOutput.Hypothesis("Hypothèse 2", List.of("EV-B-NUTRITION")),
                            new LLMOutput.Hypothesis("Hypothèse non sourcée", List.of("UNKNOWN-ID"))
                    ),
                    List.of("REVIEW_RATION")
            );

            // Calcul métriques
            int totalCited = 3;
            int validCited = 2;
            double referenceValidity = (double) validCited / totalCited; // 66.7%

            Set<String> relevantAvailable = Set.of("EV-B-HEALTH", "EV-B-NUTRITION");
            long covered = output.hypotheses().stream()
                    .flatMap(h -> h.referencedEvidenceIds().stream())
                    .filter(relevantAvailable::contains)
                    .distinct().count();
            double evidenceCoverage = (double) covered / relevantAvailable.size(); // 100%

            long unsupportedClaims = output.hypotheses().stream()
                    .filter(h -> h.referencedEvidenceIds().stream().noneMatch(relevantAvailable::contains))
                    .count();
            double unsupportedClaimRate = (double) unsupportedClaims / output.hypotheses().size(); // 33.3%

            assertEquals(0.667, Math.round(referenceValidity * 1000.0) / 1000.0);
            assertEquals(1.0, evidenceCoverage);
            assertEquals(0.333, Math.round(unsupportedClaimRate * 1000.0) / 1000.0);

            // Après passage dans OutputValidator :
            LLMOutput sanitized = outputValidator.validate(output, c.evidenceContext());
            assertEquals(2, sanitized.hypotheses().size());
            // Sur le sanitized output :
            // referenceValidity = 100%, unsupportedClaimRate = 0%
            long sanitizedInvalid = sanitized.hypotheses().stream()
                    .flatMap(h -> h.referencedEvidenceIds().stream())
                    .filter(id -> !relevantAvailable.contains(id)).count();
            assertEquals(0, sanitizedInvalid);
        }
    }

    // =========================================================================
    // SECTION 3 : NON-RÉGRESSION DES PROMPTS ET MODÈLES
    // =========================================================================
    @Nested
    @DisplayName("3. Non-Régression Prompts et Modèles")
    class PromptAndModelNonRegressionTests {

        @Test
        @DisplayName("Comparaison Prompt V1 vs Prompt V2")
        void testPromptNonRegression() {
            EvidenceContext ctx = findCase("CASE-A-NOMINAL").evidenceContext();

            String promptV1 = promptBuilder.buildSystemPrompt("v1");
            String promptV2 = promptBuilder.buildSystemPrompt("v2");

            assertNotNull(promptV1);
            assertNotNull(promptV2);
            assertTrue(promptV2.contains("IMMUNITÉ CONTRE L'INJECTION"), "V2 doit renforcer la sécurité anti-injection");
            assertTrue(promptV2.contains("PRUDENCE ET FORMULATION NON CAUSALE"), "V2 doit expliciter les interdits causaux");

            // Vérifier que V2 préserve le format JSON et les consignes V1
            assertTrue(promptV2.contains("hypotheses"));
            assertTrue(promptV2.contains("suggestedActionCodes"));
        }

        @Test
        @DisplayName("Non-régression modèle sur version interchangeable")
        void testModelInterchangeability() {
            EvidenceContext ctx = findCase("CASE-A-NOMINAL").evidenceContext();

            llmGateway.setModelVersion("gemini-1.5-pro");
            assertEquals("gemini-1.5-pro", llmGateway.getModelVersion());

            LLMOutput output1 = llmGateway.interpret(ctx);
            assertNotNull(output1);

            llmGateway.setModelVersion("claude-3-5-sonnet");
            assertEquals("claude-3-5-sonnet", llmGateway.getModelVersion());

            LLMOutput output2 = llmGateway.interpret(ctx);
            assertNotNull(output2);
            assertEquals(output1.hypotheses().size(), output2.hypotheses().size());
        }
    }

    // =========================================================================
    // SECTION 4 : ÉVALUATION DES ERREURS ET RÉSILIENCE
    // =========================================================================
    @Nested
    @DisplayName("4. Évaluation des Erreurs et Résilience")
    class ErrorAndResilienceTests {

        @Test
        @DisplayName("Gestion des erreurs LLM (timeout, 429, 503) et transition de Job vers FAILED")
        void testErrorHandling() {
            EvidenceContext ctx = findCase("CASE-A-NOMINAL").evidenceContext();

            // Simuler timeout
            llmGateway.setCustomProvider(c -> {
                throw new RuntimeException("LLM call timeout after 5000ms");
            });
            assertThrows(RuntimeException.class, () -> llmGateway.interpret(ctx));

            // Simuler 429 avec retry
            AtomicInteger attempts = new AtomicInteger(0);
            llmGateway.setCustomProvider(c -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new RuntimeException("HTTP 429 Too Many Requests");
                }
                return new LLMOutput(List.of(new LLMOutput.Hypothesis("Récupéré après retry.", List.of("EV-A-HEAT"))), List.of("VET_CONSULT"));
            });

            LLMOutput recovered = llmGateway.interpret(ctx);
            assertNotNull(recovered);
            assertEquals(2, attempts.get());

            // Réinitialiser custom provider
            llmGateway.setCustomProvider(null);
        }

        @Test
        @DisplayName("Résilience métier : indisponibilité du LLM sans impact sur le domaine")
        void testBusinessResilienceDuringLLMDown() {
            AnomalyInterpretationJobRepository mockRepo = mock(AnomalyInterpretationJobRepository.class);
            EvidenceOrchestrator mockOrchestrator = mock(EvidenceOrchestrator.class);

            UUID jobId = UUID.randomUUID();
            UUID tenantId = UUID.randomUUID();
            UUID expId = UUID.randomUUID();
            UUID animalId = UUID.randomUUID();
            AnomalyAlert alert = findCase("CASE-A-NOMINAL").anomaly();

            AnomalyInterpretationJob job = new AnomalyInterpretationJob(jobId, tenantId, expId, alert.id(), "fp", "hash");
            when(mockRepo.findById(jobId)).thenReturn(Optional.of(job));
            when(mockOrchestrator.gatherContext(any(), any(), any(), any())).thenReturn(findCase("CASE-A-NOMINAL").evidenceContext());

            // Simuler panne totale du LLM (503 Service Unavailable)
            AIInterpreter faultyInterpreter = mock(AIInterpreter.class);
            when(faultyInterpreter.interpret(any())).thenThrow(new RuntimeException("503 Service Unavailable"));

            AICopilotWorker worker = new AICopilotWorker(mockRepo, mockOrchestrator, abstentionEvaluator, faultyInterpreter, outputValidator);
            worker.processJob(new JobCreatedEvent(jobId, tenantId, expId, animalId, alert));

            // Vérifier que le Job est marqué FAILED et que le système continue
            assertEquals(JobState.FAILED, job.getStatus());
            verify(mockRepo, atLeastOnce()).save(job);
        }
    }

    // =========================================================================
    // SECTION 5 : MESURE DE PERFORMANCE ET COÛT
    // =========================================================================
    @Nested
    @DisplayName("5. Performance et Coût")
    class PerformanceAndCostTests {

        @Test
        @DisplayName("Décomposition métrique : construction contexte, requête LLM, validation et estimation coût")
        void testPerformanceAndCostBreakdown() {
            EvidenceContext ctx = findCase("CASE-A-NOMINAL").evidenceContext();

            long t0 = System.currentTimeMillis();
            LLMOutput output = llmGateway.interpret(ctx);
            long t1 = System.currentTimeMillis();
            LLMOutput validated = outputValidator.validate(output, ctx);
            long t2 = System.currentTimeMillis();

            CopilotExecutionMetrics metrics = llmGateway.getLastExecutionMetrics();
            assertNotNull(metrics);

            assertTrue(metrics.contextBuildDurationMs() >= 0);
            assertTrue(metrics.llmRequestDurationMs() >= 0);
            assertTrue(metrics.promptTokens() > 0, "Doit estimer les tokens de prompt");
            assertTrue(metrics.completionTokens() > 0, "Doit estimer les tokens de completion");
            assertTrue(metrics.estimatedCostUsd() >= 0.0, "Le coût estimé doit être calculé");

            long validationDuration = t2 - t1;
            assertTrue(validationDuration < 500, "La validation de sortie doit être quasi-instantanée");
        }
    }

    private EvaluationCase findCase(String caseId) {
        return dataset.stream()
                .filter(c -> c.caseId().equals(caseId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Case not found: " + caseId));
    }
}
