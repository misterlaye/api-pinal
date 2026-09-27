package com.dairy.apipinal.shared.security;

import org.springframework.stereotype.Service;

@Service("authz")
public class AuthorizationService {

    private final TenantContext tenantContext;

    public AuthorizationService(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    public boolean isOwner() {
        return "PROPRIETAIRE".equals(tenantContext.currentRole());
    }

    public boolean isWorkerOrOwner() {
        String role = tenantContext.currentRole();
        return "PROPRIETAIRE".equals(role) ||
               "GERANT".equals(role) ||
               "EMPLOYE".equals(role);
    }
}
