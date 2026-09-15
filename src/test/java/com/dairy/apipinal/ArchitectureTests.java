package com.dairy.apipinal;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTests {

    @Test
    void modulesShouldRespectTheirBoundaries() {

        ApplicationModules
                .of(ApiPinalApplication.class)
                .verify();
    }
}