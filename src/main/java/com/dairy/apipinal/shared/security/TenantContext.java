package com.dairy.apipinal.shared.security;

import java.util.UUID;

public interface TenantContext {

    UUID currentTenantId();

    UUID currentUserId();
}