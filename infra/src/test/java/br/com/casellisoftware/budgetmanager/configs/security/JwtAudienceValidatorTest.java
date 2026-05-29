package br.com.casellisoftware.budgetmanager.configs.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAudienceValidatorTest {

    @Test
    void validate_matchingAudience_succeeds() {
        JwtAudienceValidator validator = new JwtAudienceValidator(List.of("budgetmanager-api"));

        assertThat(validator.validate(jwtWithAudience(List.of("budgetmanager-api"))).hasErrors()).isFalse();
    }

    @Test
    void validate_missingAcceptedAudience_fails() {
        JwtAudienceValidator validator = new JwtAudienceValidator(List.of("budgetmanager-api"));

        assertThat(validator.validate(jwtWithAudience(List.of("other-client"))).hasErrors()).isTrue();
    }

    @Test
    void validate_withoutConfiguredAudiences_failsClosed() {
        JwtAudienceValidator validator = new JwtAudienceValidator(List.of());

        assertThat(validator.validate(jwtWithAudience(List.of("budgetmanager-api"))).hasErrors()).isTrue();
    }

    private static Jwt jwtWithAudience(List<String> audience) {
        return new Jwt(
                "token",
                Instant.parse("2026-05-12T00:00:00Z"),
                Instant.parse("2026-05-12T00:15:00Z"),
                Map.of("alg", "none"),
                Map.of("sub", "user-1", "aud", audience)
        );
    }
}
