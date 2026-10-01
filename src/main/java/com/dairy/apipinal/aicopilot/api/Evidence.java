package com.dairy.apipinal.aicopilot.api;

import java.time.OffsetDateTime;
import java.util.Map;

public interface Evidence {
    String evidenceId();
    String contentDigest();
    String sourceModule();
    String entityType();
    String entityId();
    OffsetDateTime observationDate();
    String factType();
    Map<String, String> values();
    String provenance();
    TemporalRelation temporalRelation();
    SupportLevel supportLevel();
}
