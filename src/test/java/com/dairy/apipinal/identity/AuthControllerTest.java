package com.dairy.apipinal.identity;

import com.dairy.apipinal.identity.application.AuthService;
import com.dairy.apipinal.identity.infrastructure.web.AuthController;
import com.dairy.apipinal.shared.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @WithMockUser
    void shouldRequestOtp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telephone": "+221770001122"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).requestOtp("+221770001122");
    }

    @Test
    @WithMockUser
    void shouldRejectOtpRequestWithoutPhone() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telephone": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldVerifyOtp() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthService.AuthResponse response = new AuthService.AuthResponse(
                "access-token-123", "refresh-token-456", 900, userId
        );
        when(authService.verifyOtp("+221770001122", "123456")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telephone": "+221770001122", "code": "123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @WithMockUser
    void shouldRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthService.AuthResponse response = new AuthService.AuthResponse(
                "new-access", "new-refresh", 900, userId
        );
        when(authService.refreshToken("old-refresh-token")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "old-refresh-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    @WithMockUser
    void shouldLogout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "refresh-to-revoke"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).logout("refresh-to-revoke");
    }
}
