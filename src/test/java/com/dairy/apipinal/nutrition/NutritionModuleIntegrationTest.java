package com.dairy.apipinal.nutrition;

import com.dairy.apipinal.animal.api.AnimalQueries;
import com.dairy.apipinal.shared.security.TenantContext;
import com.dairy.apipinal.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class NutritionModuleIntegrationTest {

    @MockitoBean
    private TenantContext tenantContext;

    @MockitoBean
    private AnimalQueries animalQueries;

    @Test
    void shouldLoadNutritionModule() {
        assertThat(tenantContext).isNotNull();
        assertThat(animalQueries).isNotNull();
    }
}