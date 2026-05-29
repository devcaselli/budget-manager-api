package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenRevocationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects JWTs whose {@code jti} claim is present in the revocation store.
 *
 * <p>If the token has no {@code jti} claim (e.g. tokens issued before N-2 was deployed),
 * validation passes — backward compatibility is preserved.</p>
 */
@RequiredArgsConstructor
public class JtiRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_ERROR =
            new OAuth2Error("invalid_token", "Token has been revoked", null);

    private final TokenRevocationPort revocationPort;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String jti = jwt.getId();
        if (jti == null) {
            return OAuth2TokenValidatorResult.success();
        }
        if (revocationPort.isRevoked(jti)) {
            return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
