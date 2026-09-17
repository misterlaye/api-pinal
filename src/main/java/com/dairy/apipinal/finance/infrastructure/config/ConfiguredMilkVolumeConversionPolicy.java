package com.dairy.apipinal.finance.infrastructure.config;

import com.dairy.apipinal.finance.application.MilkVolumeConversionPolicy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ConfiguredMilkVolumeConversionPolicy
        implements MilkVolumeConversionPolicy {

    private static final String PROPERTY =
            "finance.lait.litres-par-kg";

    private final Environment environment;

    public ConfiguredMilkVolumeConversionPolicy(
            Environment environment
    ) {
        this.environment = environment;
    }

    @Override
    public BigDecimal convertKgToLitres(
            BigDecimal volumeKg
    ) {
        if (volumeKg == null || volumeKg.signum() < 0) {
            throw new IllegalArgumentException(
                    "Le volume de lait en kg doit être supérieur ou égal à zéro."
            );
        }

        String configuredValue =
                environment.getProperty(PROPERTY);

        if (configuredValue == null || configuredValue.isBlank()) {
            throw new IllegalStateException(
                    "Le facteur de conversion kg vers litre n'est pas configuré : "
                            + PROPERTY
            );
        }

        BigDecimal litresParKg;

        try {
            litresParKg = new BigDecimal(
                    configuredValue.trim()
            );
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Le facteur de conversion kg vers litre est invalide.",
                    exception
            );
        }

        if (litresParKg.signum() <= 0) {
            throw new IllegalStateException(
                    "Le facteur de conversion kg vers litre doit être strictement positif."
            );
        }

        return volumeKg.multiply(litresParKg);
    }
}