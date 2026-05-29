package br.com.casellisoftware.budgetmanager.application.auth.boundary;

import java.time.Instant;

/**
 * Port for token revocation — stores and queries revoked JWT identifiers.
 *
 * <p>Implementations are responsible for persisting {@code jti} values until
 * the token's natural expiry, after which entries may be discarded.</p>
 */
public interface TokenRevocationPort {

    /**
     * Marks a JWT as revoked.
     *
     * @param jti       the JWT ID claim value (must be non-null)
     * @param expiresAt the token's expiry instant — used to bound storage lifetime
     */
    void revoke(String jti, Instant expiresAt);

    /**
     * Returns {@code true} if the given {@code jti} has been revoked.
     *
     * @implNote Time complexity: O(1) — single indexed document lookup.
     */
    boolean isRevoked(String jti);
}
