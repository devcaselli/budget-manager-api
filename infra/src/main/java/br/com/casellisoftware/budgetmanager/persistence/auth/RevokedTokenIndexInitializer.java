package br.com.casellisoftware.budgetmanager.persistence.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Ensures TTL indexes on auth token collections exist at application startup.
 *
 * <p>Covers {@code revoked_tokens} (N-2) and {@code refresh_tokens} (N-3).
 * Using {@link ApplicationReadyEvent} rather than {@code @PostConstruct} guarantees
 * the MongoDB connection is fully established before index creation is attempted.
 * Each {@code ensureIndex} call is idempotent — MongoDB ignores duplicate requests.</p>
 *
 * <p>This approach is preferred over {@code spring.data.mongodb.auto-index-creation=true}
 * because it targets only these specific indexes without enabling global auto-creation,
 * which can cause issues with large production collections.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevokedTokenIndexInitializer {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAuthTokenTtlIndexes() {
        Index ttlIndex = new Index("expiresAt", Sort.Direction.ASC).expire(0);

        mongoTemplate.indexOps(RevokedTokenDocument.class).ensureIndex(ttlIndex);
        log.info("TTL index on revoked_tokens.expiresAt ensured");

        mongoTemplate.indexOps(RefreshTokenDocument.class).ensureIndex(ttlIndex);
        log.info("TTL index on refresh_tokens.expiresAt ensured");
    }
}
