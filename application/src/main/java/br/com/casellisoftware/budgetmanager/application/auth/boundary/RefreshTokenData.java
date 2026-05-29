package br.com.casellisoftware.budgetmanager.application.auth.boundary;

/**
 * Projection returned by {@link RefreshTokenPort#findByToken(String)}.
 *
 * <p>Carries just enough data to re-issue an access token without an extra
 * database round-trip to the user collection.</p>
 */
public record RefreshTokenData(String userId, String email) {
}
