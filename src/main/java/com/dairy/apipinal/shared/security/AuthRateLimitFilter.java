package com.dairy.apipinal.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite le nombre d'appels aux endpoints d'authentification ({@code /api/v1/auth/**})
 * par adresse cliente et par endpoint, sur une fenêtre fixe d'une minute.
 * <p>
 * Complète le verrouillage par compte (PIN) et par code (OTP) : il freine la pulvérisation
 * de tentatives sur de nombreux numéros et l'envoi massif de SMS.
 * L'état est conservé en mémoire, par instance : derrière un reverse proxy, l'adresse cliente
 * doit être transmise correctement (server.forward-headers-strategy).
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxRequestsPerMinute;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(int maxRequestsPerMinute) {
        this(maxRequestsPerMinute, Clock.systemUTC());
    }

    AuthRateLimitFilter(int maxRequestsPerMinute, Clock clock) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return maxRequestsPerMinute <= 0 || !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long now = clock.millis();
        long windowStart = now - (now % WINDOW_MILLIS);
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(window -> window.start < windowStart);
        }

        Window window = windows.compute(key, (k, existing) ->
                existing == null || existing.start != windowStart ? new Window(windowStart) : existing);

        if (window.count.incrementAndGet() > maxRequestsPerMinute) {
            long retryAfterSeconds = Math.max(1, (windowStart + WINDOW_MILLIS - now) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Trop de tentatives\",\"status\":429,"
                    + "\"detail\":\"Trop de tentatives. Réessayez dans " + retryAfterSeconds + " secondes.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class Window {
        private final long start;
        private final AtomicInteger count = new AtomicInteger();

        private Window(long start) {
            this.start = start;
        }
    }
}
