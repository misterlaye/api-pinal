package com.dairy.apipinal.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private static final Instant T0 = Instant.parse("2026-10-16T10:00:05Z");

    @Test
    void blocksAuthCallsBeyondLimitWithinTheSameMinute() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(3, Clock.fixed(T0, ZoneOffset.UTC));

        for (int i = 0; i < 3; i++) {
            assertThat(call(filter, "/api/v1/auth/pin/verify", "10.0.0.1").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = call(filter, "/api/v1/auth/pin/verify", "10.0.0.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("\"detail\"");

        // Autre client, autre endpoint d'authentification, endpoint métier : non concernés.
        assertThat(call(filter, "/api/v1/auth/pin/verify", "10.0.0.2").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/auth/otp/request", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/animals", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void counterResetsOnNextWindow() throws Exception {
        MutableClock clock = new MutableClock(T0);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, clock);

        assertThat(call(filter, "/api/v1/auth/otp/verify", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(call(filter, "/api/v1/auth/otp/verify", "10.0.0.1").getStatus()).isEqualTo(429);
        clock.instant = T0.plusSeconds(60);
        assertThat(call(filter, "/api/v1/auth/otp/verify", "10.0.0.1").getStatus()).isEqualTo(200);
    }

    @Test
    void zeroDisablesTheLimit() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(0, Clock.fixed(T0, ZoneOffset.UTC));
        for (int i = 0; i < 50; i++) {
            assertThat(call(filter, "/api/v1/auth/pin/verify", "10.0.0.1").getStatus()).isEqualTo(200);
        }
    }

    private static MockHttpServletResponse call(AuthRateLimitFilter filter, String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
