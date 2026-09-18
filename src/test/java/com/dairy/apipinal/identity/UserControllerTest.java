package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.application.RegisterUser;
import com.dairy.apipinal.identity.domain.Utilisateur;
import com.dairy.apipinal.identity.infrastructure.persistence.UtilisateurRepository;
import com.dairy.apipinal.identity.infrastructure.web.UserController;
import com.dairy.apipinal.shared.infrastructure.web.GlobalExceptionHandler;
import com.dairy.apipinal.shared.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUser registerUser;

    @MockitoBean
    private UtilisateurRepository utilisateurRepository;

    @MockitoBean
    private TenantContext tenantContext;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(tenantContext.currentUserId()).thenReturn(userId);
    }

    @Test
    @WithMockUser
    void shouldRegisterOrUpdateUser() throws Exception {
        Utilisateur user = new Utilisateur(userId, "+221770000000", "Diallo", "Abdoulaye");

        when(registerUser.execute(any(RegisterUser.Command.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/identity/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "telephone": "+221770000000",
                                  "nom": "Diallo",
                                  "prenom": "Abdoulaye",
                                  "email": "test@pinal.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Diallo"))
                .andExpect(jsonPath("$.prenom").value("Abdoulaye"));
    }

    @Test
    @WithMockUser
    void shouldGetCurrentUser() throws Exception {
        Utilisateur user = new Utilisateur(userId, "+221770000000", "Diallo", "Abdoulaye");
        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/identity/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Diallo"));
    }
}
