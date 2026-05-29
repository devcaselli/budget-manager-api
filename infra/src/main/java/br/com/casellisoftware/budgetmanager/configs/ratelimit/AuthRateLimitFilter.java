package br.com.casellisoftware.budgetmanager.configs.ratelimit;

import tools.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that applies per-IP (and per-email for token endpoint) rate limiting
 * using Bucket4j token-bucket algorithm with Caffeine-backed cache.
 *
 * <p>Two independent limits are enforced on {@code POST /auth/token}:</p>
 * <ol>
 *   <li>IP bucket — {@code ipRequestsPerMinute} tokens refilled every minute.</li>
 *   <li>Email bucket — {@code emailRequestsPerHour} tokens refilled every hour.
 *       Email is extracted from the JSON request body without consuming the stream
 *       (uses {@link CachedBodyHttpServletRequest}).</li>
 * </ol>
 *
 * <p>One limit is enforced on {@code POST /auth/register}:</p>
 * <ol>
 *   <li>IP bucket — {@code ipRequestsPerHour} tokens refilled every hour.</li>
 * </ol>
 *
 * <p>On limit exceeded: {@code 429 Too Many Requests} + {@code Retry-After} header
 * + RFC 7807 {@code ProblemDetail} JSON body.</p>
 *
 * <p>Cache is bounded by {@code cacheMaxSize} and evicts entries after
 * {@code cacheExpirySeconds} of inactivity — prevents OOM on IP-rotating attackers.</p>
 *
 * @implNote Time complexity: O(1) per request. Space: O(min(activeKeys, cacheMaxSize)).
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final String PATH_TOKEN    = "/auth/token";
    private static final String PATH_REGISTER = "/auth/register";

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;

    /** Cache key prefix to avoid collision between IP and email buckets. */
    private static final String PREFIX_IP_TOKEN    = "ip:token:";
    private static final String PREFIX_EMAIL_TOKEN = "email:token:";
    private static final String PREFIX_IP_REGISTER = "ip:register:";

    private final Cache<String, Bucket> bucketCache;

    public AuthRateLimitFilter(RateLimitProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.bucketCache = Caffeine.newBuilder()
                .maximumSize(props.cacheMaxSize())
                .expireAfterAccess(props.cacheExpirySeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.enabled()) return true;
        String path = request.getServletPath();
        return !PATH_TOKEN.equals(path) && !PATH_REGISTER.equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getServletPath();
        String ip   = resolveIp(request);

        if (PATH_TOKEN.equals(path)) {
            // Wrap request to allow body re-read (email extraction)
            CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(request);

            // 1. IP bucket
            Bucket ipBucket = bucketCache.get(PREFIX_IP_TOKEN + ip,
                    k -> newBucket(props.token().ipRequestsPerMinute(), Duration.ofMinutes(1)));
            if (!ipBucket.tryConsume(1)) {
                long waitSeconds = ipBucket.getAvailableTokens() == 0
                        ? 60 : ipBucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000L;
                log.warn("rate-limit: IP {} throttled on /auth/token", ip);
                rejectWithTooManyRequests(response, waitSeconds);
                return;
            }

            // 2. Email bucket (best-effort — if email cannot be parsed, skip email check)
            String email = extractEmail(cached);
            if (email != null && !email.isBlank()) {
                Bucket emailBucket = bucketCache.get(PREFIX_EMAIL_TOKEN + email.toLowerCase(),
                        k -> newBucket(props.token().emailRequestsPerHour(), Duration.ofHours(1)));
                if (!emailBucket.tryConsume(1)) {
                    long waitSeconds = emailBucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000L;
                    log.warn("rate-limit: email throttled on /auth/token (ip={})", ip);
                    rejectWithTooManyRequests(response, waitSeconds);
                    return;
                }
            }

            chain.doFilter(cached, response);

        } else if (PATH_REGISTER.equals(path)) {
            Bucket ipBucket = bucketCache.get(PREFIX_IP_REGISTER + ip,
                    k -> newBucket(props.register().ipRequestsPerHour(), Duration.ofHours(1)));
            if (!ipBucket.tryConsume(1)) {
                long waitSeconds = ipBucket.estimateAbilityToConsume(1).getNanosToWaitForRefill() / 1_000_000_000L;
                log.warn("rate-limit: IP {} throttled on /auth/register", ip);
                rejectWithTooManyRequests(response, waitSeconds);
                return;
            }

            chain.doFilter(request, response);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Bucket newBucket(long tokens, Duration period) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(tokens)
                        .refillGreedy(tokens, period)
                        .build())
                .build();
    }

    private String resolveIp(HttpServletRequest request) {
        if (props.trustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                // Take the first (leftmost) IP — the original client
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String extractEmail(CachedBodyHttpServletRequest request) {
        try {
            var node = objectMapper.readTree(request.getCachedBody());
            var emailNode = node.get("email");
            return emailNode != null ? emailNode.asText(null) : null;
        } catch (Exception e) {
            log.debug("rate-limit: could not parse email from request body — skipping email bucket");
            return null;
        }
    }

    private void rejectWithTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        problem.setTitle("Too Many Requests");
        problem.setDetail("Rate limit exceeded. Please slow down and try again later.");
        problem.setType(URI.create("about:blank"));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(Math.max(1L, retryAfterSeconds)));
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
