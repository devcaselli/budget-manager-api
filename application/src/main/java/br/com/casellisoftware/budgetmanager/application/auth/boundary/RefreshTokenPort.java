package br.com.casellisoftware.budgetmanager.application.auth.boundary;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for opaque refresh token lifecycle management.
 *
 * <p>Refresh tokens are stored as opaque UUIDs in a TTL-indexed MongoDB collection.
 * Each token is single-use: consuming it immediately invalidates it and the caller
 * is responsible for issuing a replacement.</p>
 */
public interface RefreshTokenPort {

    /**
     * Persists a new refresh token bound to the given user.
     *
     * @param token     the opaque token value (UUID)
     * @param userId    owner of the token
     * @param email     owner's email — carried so the use case can re-issue an access token
     *                  without a database round-trip to the user collection
     * @param expiresAt token expiry instant — used as TTL index target
     */
    void save(String token, String userId, String email, Instant expiresAt);

    /**
     * Looks up a refresh token by its value.
     *
     * <p>Returns {@link Optional#empty()} when the token does not exist or has
     * already been consumed/expired.</p>
     *
     * @implNote Time complexity: O(1) — single indexed document lookup.
     */
    Optional<RefreshTokenData> findByToken(String token);

    /**
     * Deletes a refresh token, rendering it permanently unusable.
     *
     * <p>Must be called before issuing a replacement to enforce single-use semantics.</p>
     */
    void delete(String token);
}
