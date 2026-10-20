package com.dairy.apipinal.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    private final TokenValidator tokenValidator;
    private final int authRequestsPerMinute;

    public SecurityConfiguration(
            TokenValidator tokenValidator,
            @Value("${pinal.security.auth-rate-limit.requests-per-minute:20}") int authRequestsPerMinute
    ) {
        this.tokenValidator = tokenValidator;
        this.authRequestsPerMinute = authRequestsPerMinute;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR, jakarta.servlet.DispatcherType.FORWARD).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> httpBasic.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeProblem(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise",
                                        "Session absente ou expirée. Veuillez vous reconnecter."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeProblem(response, HttpServletResponse.SC_FORBIDDEN, "Accès refusé",
                                        "Vous n'avez pas les droits nécessaires pour cette opération."))
                )
                .addFilterBefore(new AuthRateLimitFilter(authRequestsPerMinute), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(tokenValidator), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static void writeProblem(HttpServletResponse response, int status, String title, String detail)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
