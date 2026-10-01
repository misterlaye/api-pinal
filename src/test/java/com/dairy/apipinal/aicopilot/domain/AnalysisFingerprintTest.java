package com.dairy.apipinal.aicopilot.domain;

import org.junit.jupiter.api.Test;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisFingerprintTest {

    @Test
    void fingerprint_is_deterministic() {
        UUID tenantId = UUID.randomUUID();
        String anomalyId = "anom-1";
        String hash = "hash123";
        
        AnalysisFingerprint f1 = AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v1", "v1", "v1");
        AnalysisFingerprint f2 = AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v1", "v1", "v1");
        
        assertEquals(f1.value(), f2.value());
    }

    @Test
    void fingerprint_changes_with_any_param() {
        UUID tenantId = UUID.randomUUID();
        String anomalyId = "anom-1";
        String hash = "hash123";
        
        AnalysisFingerprint base = AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v1", "v1", "v1");
        
        assertNotEquals(base.value(), AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v2", "v1", "v1", "v1").value());
        assertNotEquals(base.value(), AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v2", "v1", "v1").value());
        assertNotEquals(base.value(), AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v1", "v2", "v1").value());
        assertNotEquals(base.value(), AnalysisFingerprint.compute(tenantId, anomalyId, hash, "v1", "v1", "v1", "v2").value());
    }
}
