package br.com.casellisoftware.budgetmanager.configs.ratelimit;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AuthRateLimitFilter filterWithLimits(long tokenIpRpm, long tokenEmailRph, long registerIpRph) {
        RateLimitProperties props = new RateLimitProperties(
                true, false, 10_000L, 3600L,
                new RateLimitProperties.Token(tokenIpRpm, tokenEmailRph),
                new RateLimitProperties.Register(registerIpRph)
        );
        return new AuthRateLimitFilter(props, MAPPER);
    }

    private AuthRateLimitFilter disabledFilter() {
        RateLimitProperties props = new RateLimitProperties(
                false, false, 10_000L, 3600L,
                new RateLimitProperties.Token(1L, 1L),
                new RateLimitProperties.Register(1L)
        );
        return new AuthRateLimitFilter(props, MAPPER);
    }

    private MockHttpServletRequest tokenRequest(String ip, String email) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/token");
        req.setServletPath("/auth/token");
        req.setRemoteAddr(ip);
        req.setContentType("application/json");
        req.setContent(("{\"email\":\"" + email + "\",\"password\":\"secret\"}").getBytes());
        return req;
    }

    private MockHttpServletRequest registerRequest(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/register");
        req.setServletPath("/auth/register");
        req.setRemoteAddr(ip);
        req.setContentType("application/json");
        req.setContent("{\"email\":\"u@u.com\",\"password\":\"password123\"}".getBytes());
        return req;
    }

    private MockHttpServletRequest otherRequest(String path) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
        req.setServletPath(path);
        return req;
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void token_firstRequest_passesThrough() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(5, 5, 3);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(tokenRequest("1.2.3.4", "user@test.com"), response, chain);

        verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isNotEqualTo(429);
    }

    @Test
    void token_exceedsIpLimit_returns429() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(2, 100, 100);
        FilterChain chain = mock(FilterChain.class);

        // consume 2 allowed tokens
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilter(tokenRequest("5.5.5.5", "a@b.com"), r, chain);
        }

        // 3rd request must be throttled
        MockHttpServletResponse throttled = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("5.5.5.5", "a@b.com"), throttled, chain);

        assertThat(throttled.getStatus()).isEqualTo(429);
        assertThat(throttled.getHeader("Retry-After")).isNotNull();
        assertThat(throttled.getContentType()).contains("application/problem+json");
        // chain called only twice (the 2 allowed), not on the throttled request
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void token_exceedsEmailLimit_returns429() throws Exception {
        // High IP limit, low email limit
        AuthRateLimitFilter filter = filterWithLimits(100, 2, 100);
        FilterChain chain = mock(FilterChain.class);
        String sameEmail = "victim@test.com";

        // Consume email budget from 2 different IPs (to bypass IP limit)
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("10.0.0.1", sameEmail), r1, chain);
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("10.0.0.2", sameEmail), r2, chain);

        // 3rd request from yet another IP — IP bucket fresh but email bucket exhausted
        MockHttpServletResponse throttled = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("10.0.0.3", sameEmail), throttled, chain);

        assertThat(throttled.getStatus()).isEqualTo(429);
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void register_exceedsIpLimit_returns429() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(100, 100, 2);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse r = new MockHttpServletResponse();
            filter.doFilter(registerRequest("9.9.9.9"), r, chain);
        }

        MockHttpServletResponse throttled = new MockHttpServletResponse();
        filter.doFilter(registerRequest("9.9.9.9"), throttled, chain);

        assertThat(throttled.getStatus()).isEqualTo(429);
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void differentIps_independentBuckets() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(1, 100, 100);
        FilterChain chain = mock(FilterChain.class);

        // IP A consumes its 1 token
        MockHttpServletResponse ra = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("1.1.1.1", "a@b.com"), ra, chain);

        // IP B still has its own fresh bucket
        MockHttpServletResponse rb = new MockHttpServletResponse();
        filter.doFilter(tokenRequest("2.2.2.2", "c@d.com"), rb, chain);

        assertThat(rb.getStatus()).isNotEqualTo(429);
        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void disabled_filter_alwaysPassesThrough() throws Exception {
        AuthRateLimitFilter filter = disabledFilter();
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilter(tokenRequest("1.2.3.4", "x@y.com"), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(10)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nonAuthPath_skipsFilter() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(1, 1, 1);
        FilterChain chain = mock(FilterChain.class);

        // Hit /wallets — should never be intercepted
        for (int i = 0; i < 5; i++) {
            filter.doFilter(otherRequest("/wallets"), new MockHttpServletResponse(), chain);
        }

        verify(chain, times(5)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void token_missingEmailInBody_ipBucketStillEnforced() throws Exception {
        AuthRateLimitFilter filter = filterWithLimits(1, 100, 100);
        FilterChain chain = mock(FilterChain.class);

        // Request without email field
        MockHttpServletRequest noEmail = new MockHttpServletRequest("POST", "/auth/token");
        noEmail.setServletPath("/auth/token");
        noEmail.setRemoteAddr("3.3.3.3");
        noEmail.setContentType("application/json");
        noEmail.setContent("{\"password\":\"abc\"}".getBytes());

        MockHttpServletResponse r1 = new MockHttpServletResponse();
        filter.doFilter(noEmail, r1, chain);
        assertThat(r1.getStatus()).isNotEqualTo(429);

        // Reset stream — need a fresh request object (IP same → bucket exhausted)
        MockHttpServletRequest noEmail2 = new MockHttpServletRequest("POST", "/auth/token");
        noEmail2.setServletPath("/auth/token");
        noEmail2.setRemoteAddr("3.3.3.3");
        noEmail2.setContentType("application/json");
        noEmail2.setContent("{\"password\":\"abc\"}".getBytes());

        MockHttpServletResponse r2 = new MockHttpServletResponse();
        filter.doFilter(noEmail2, r2, chain);
        assertThat(r2.getStatus()).isEqualTo(429);
    }

    @Test
    void token_trustForwardedFor_usesForwardedIp() throws Exception {
        RateLimitProperties props = new RateLimitProperties(
                true, true, 10_000L, 3600L,
                new RateLimitProperties.Token(1L, 100L),
                new RateLimitProperties.Register(100L)
        );
        AuthRateLimitFilter filter = new AuthRateLimitFilter(props, MAPPER);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest req1 = tokenRequest("proxy.ip", "e@f.com");
        req1.addHeader("X-Forwarded-For", "8.8.8.8, proxy.ip");
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        filter.doFilter(req1, r1, chain);

        // Same forwarded IP — bucket exhausted (limit=1)
        MockHttpServletRequest req2 = tokenRequest("proxy.ip", "other@f.com");
        req2.addHeader("X-Forwarded-For", "8.8.8.8, proxy.ip");
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        filter.doFilter(req2, r2, chain);

        assertThat(r2.getStatus()).isEqualTo(429);
    }
}
