package com.dairy.apipinal.aicopilot.domain;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilsTest {

    @Test
    void sha256_is_deterministic() {
        String input = "test_string";
        assertEquals(HashUtils.sha256(input), HashUtils.sha256(input));
    }

    @Test
    void formatDateTimeCanonical_ignores_offset_for_utc() {
        OffsetDateTime dt1 = OffsetDateTime.of(2026, 9, 30, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime dt2 = OffsetDateTime.of(2026, 9, 30, 14, 0, 0, 0, ZoneOffset.ofHours(2));
        
        // They represent the exact same instant
        assertEquals(HashUtils.formatDateTimeCanonical(dt1), HashUtils.formatDateTimeCanonical(dt2));
    }
}
