package br.com.casellisoftware.budgetmanager.persistence.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Persisted opaque refresh token.
 *
 * <p>A TTL index on {@code expiresAt} (expireAfterSeconds = 0) removes expired
 * documents automatically. Index is created by {@link RevokedTokenIndexInitializer}
 * and its counterpart for this collection at startup.</p>
 */
@Document(collection = "refresh_tokens")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenDocument {

    /** Opaque UUID token value — used as the document identifier. */
    @Id
    private String token;

    private String userId;
    private String email;

    /** Token expiry — TTL index target field. */
    private Instant expiresAt;
}
