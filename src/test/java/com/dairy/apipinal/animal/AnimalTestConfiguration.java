package com.dairy.apipinal.animal;

import com.dairy.apipinal.shared.security.TenantContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@TestConfiguration(proxyBeanMethods = false)
public class AnimalTestConfiguration {

    private static final UUID TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Bean
    TenantContext tenantContext() {
        return new TenantContext() {

            @Override
            public UUID currentTenantId() {
                return TENANT_ID;
            }

            @Override
            public UUID currentUserId() {
                return USER_ID;
            }
        };
    }
}