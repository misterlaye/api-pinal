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
    public ResponseEntity<UserResponse> registerOrUpdateUser(
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
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UUID userId = tenantContext.currentUserId();
        return utilisateurRepository.findById(userId)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Profil exposé à l'utilisateur. N'expose jamais l'empreinte ni l'état du PIN.
     */
    public record UserResponse(
            UUID id,
            String telephone,
            String email,
            String nom,
            String prenom,
            String statut
    ) {
        static UserResponse from(Utilisateur user) {
            return new UserResponse(
                    user.getId(),
                    user.getTelephone(),
                    user.getEmail(),
                    user.getNom(),
                    user.getPrenom(),
                    user.getStatut() != null ? user.getStatut().name() : null
            );
        }
    }
}
