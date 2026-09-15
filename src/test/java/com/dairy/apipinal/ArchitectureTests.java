package com.dairy.apipinal;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTests {

    @Test
    void applicationModulesShouldBeValid() {
        ApplicationModules.of(ApiPinalApplication.class)
                .verify();
    }

    @Test
    void displayModules() {
        var modules = ApplicationModules.of(ApiPinalApplication.class);

        modules.forEach(System.out::println);
    }
}