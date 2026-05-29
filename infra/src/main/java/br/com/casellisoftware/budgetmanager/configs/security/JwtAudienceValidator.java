package br.com.casellisoftware.budgetmanager.configs.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> acceptedAudiences;

    public JwtAudienceValidator(Collection<String> acceptedAudiences) {
        this.acceptedAudiences = acceptedAudiences == null
                ? List.of()
                : acceptedAudiences.stream()
                .filter(audience -> audience != null && !audience.isBlank())
                .toList();
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (acceptedAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(error("No accepted JWT audiences configured"));
        }
        boolean matches = token.getAudience().stream().anyMatch(acceptedAudiences::contains);
        if (matches) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(error("JWT audience is not accepted"));
    }

    private static OAuth2Error error(String description) {
        return new OAuth2Error("invalid_token", description, null);
    }
}
