package com.dairy.apipinal.shared.security;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("dev")
public class DevelopmentTenantContext implements TenantContext {

    private static final UUID TENANT_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final UUID USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID currentTenantId() {
        return TENANT_ID;
    }

    @Override
    public UUID currentUserId() {
        return USER_ID;
    }
}