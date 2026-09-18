package com.dairy.apipinal.nutrition;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class NutritionArchitectureTest {

    @Test
    void applicationModulesShouldBeValid() {

        ApplicationModules modules =
                ApplicationModules.of(
                        com.dairy.apipinal.ApiPinalApplication.class
                );

        modules.verify();

        assertThat(modules)
                .isNotNull();
    }
}