package com.dairy.apipinal.identity.infrastructure.web;

import com.dairy.apipinal.identity.application.RegisterUser;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import com.dairy.apipinal.identity.infrastructure.web.dto.RegisterUserRequest;
import com.dairy.apipinal.shared.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity/users")
public class UserController {

    private final RegisterUser registerUser;
    private final UtilisateurRepository utilisateurRepository;
    private final TenantContext tenantContext;

    public UserController(
            RegisterUser registerUser,
            UtilisateurRepository utilisateurRepository,
            TenantContext tenantContext
    ) {
        this.registerUser = registerUser;
        this.utilisateurRepository = utilisateurRepository;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/me")
    public ResponseEntity<Utilisateur> registerOrUpdateUser(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        UUID userId = tenantContext.currentUserId();
        Utilisateur user = registerUser.execute(new RegisterUser.Command(
                userId,
                request.telephone(),
                request.nom(),
                request.prenom(),
                request.email()
        ));
        return ResponseEntity.ok(user);
    }

    @GetMapping("/me")
    public ResponseEntity<Utilisateur> getCurrentUser() {
        UUID userId = tenantContext.currentUserId();
        return utilisateurRepository.findById(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
