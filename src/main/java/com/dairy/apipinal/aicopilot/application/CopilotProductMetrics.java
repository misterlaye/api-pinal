package com.dairy.apipinal.aicopilot.application;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CopilotProductMetrics {

    private final MeterRegistry meterRegistry;

    public CopilotProductMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void requested(boolean refresh) {
        meterRegistry.counter("pinal.ai.copilot.requests", "refresh", Boolean.toString(refresh)).increment();
    }

    public void completed(String outcome) {
        meterRegistry.counter("pinal.ai.copilot.outcomes", "status", outcome).increment();
    }
}
