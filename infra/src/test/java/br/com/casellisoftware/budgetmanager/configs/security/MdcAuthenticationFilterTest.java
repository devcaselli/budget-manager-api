package br.com.casellisoftware.budgetmanager.configs.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MdcAuthenticationFilterTest {

    private final MdcAuthenticationFilter filter = new MdcAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.remove(MdcAuthenticationFilter.OWNER_ID_KEY);
    }

    @Test
    void doFilterInternal_withJwtAuthentication_putsOwnerIdDuringRequestAndClearsAfterwards() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("user-123"), List.of()));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) ->
                assertEquals("user-123", MDC.get(MdcAuthenticationFilter.OWNER_ID_KEY)));

        assertNull(MDC.get(MdcAuthenticationFilter.OWNER_ID_KEY));
    }

    @Test
    void doFilterInternal_withoutJwtAuthentication_removesStaleOwnerIdDuringRequestAndRestoresAfterwards() throws Exception {
        MDC.put(MdcAuthenticationFilter.OWNER_ID_KEY, "stale-owner");

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (request, response) ->
                assertNull(MDC.get(MdcAuthenticationFilter.OWNER_ID_KEY)));

        assertEquals("stale-owner", MDC.get(MdcAuthenticationFilter.OWNER_ID_KEY));
    }

    private static Jwt jwt(String subject) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .build();
    }
}
