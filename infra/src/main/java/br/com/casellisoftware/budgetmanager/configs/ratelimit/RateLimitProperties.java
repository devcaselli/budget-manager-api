package br.com.casellisoftware.budgetmanager.configs.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Rate-limit configuration bound from {@code app.rate-limit.*}.
 *
 * <p>All duration fields are in seconds. Limits are per-IP (and optionally per-email
 * for the token endpoint). Set {@code enabled=false} to disable entirely (dev/test).</p>
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(

        /**
         * Master switch. When false the filter is a no-op.
         * Default: true (production-safe).
         */
        boolean enabled,

        /**
         * Whether to trust the {@code X-Forwarded-For} header for IP extraction.
         * Enable only when the app sits behind a trusted reverse proxy.
         * Default: false.
         */
        boolean trustForwardedFor,

        /**
         * Maximum number of distinct IP keys kept in the Caffeine cache.
         * Entries expire after {@code cacheExpirySeconds} of inactivity.
         * Default: 10_000.
         */
        long cacheMaxSize,

        /**
         * Inactivity TTL (seconds) after which a cache entry is evicted.
         * Should be >= max window across all limits.
         * Default: 3600 (1 hour).
         */
        long cacheExpirySeconds,

        Token token,
        Register register

) {

    public record Token(
            /** Requests per minute per IP on POST /auth/token. Default: 20. */
            long ipRequestsPerMinute,
            /** Requests per hour per email on POST /auth/token. Default: 10. */
            long emailRequestsPerHour
    ) {}

    public record Register(
            /** Requests per hour per IP on POST /auth/register. Default: 5. */
            long ipRequestsPerHour
    ) {}

    /** Defaults applied when properties are absent. */
    public static RateLimitProperties defaults() {
        return new RateLimitProperties(
                true,
                false,
                10_000L,
                3600L,
                new Token(20L, 10L),
                new Register(5L)
        );
    }
}
