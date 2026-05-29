package br.com.casellisoftware.budgetmanager.persistence.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persisted record of a revoked JWT.
 *
 * <p>A MongoDB TTL index on {@code expiresAt} (expireAfterSeconds = 0) automatically
 * removes documents once the token's natural expiry has passed — no cleanup job needed.</p>
 */
@Document(collection = "revoked_tokens")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RevokedTokenDocument {

    /** JWT ID claim ({@code jti}) — used as the document identifier. */
    @Id
    private String jti;

    /** Token expiry instant — TTL index target field. */
    private Instant expiresAt;
}
