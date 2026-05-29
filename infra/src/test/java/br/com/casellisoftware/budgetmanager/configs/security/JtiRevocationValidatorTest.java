package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenRevocationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JtiRevocationValidatorTest {

    private static final String JTI = "550e8400-e29b-41d4-a716-446655440000";

    @Mock private TokenRevocationPort revocationPort;
    @Mock private Jwt jwt;

    private JtiRevocationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JtiRevocationValidator(revocationPort);
    }

    // -----------------------------------------------------------------------
    // Revoked token
    // -----------------------------------------------------------------------

    @Test
    void validate_revokedJti_returnsFailure() {
        when(jwt.getId()).thenReturn(JTI);
        when(revocationPort.isRevoked(JTI)).thenReturn(true);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting("errorCode")
                .containsExactly("invalid_token");
    }

    // -----------------------------------------------------------------------
    // Valid (non-revoked) token
    // -----------------------------------------------------------------------

    @Test
    void validate_nonRevokedJti_returnsSuccess() {
        when(jwt.getId()).thenReturn(JTI);
        when(revocationPort.isRevoked(JTI)).thenReturn(false);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    // -----------------------------------------------------------------------
    // Backward compatibility — token without jti (pre-N-2)
    // -----------------------------------------------------------------------

    @Test
    void validate_nullJti_returnsSuccessWithoutCallingRevocationPort() {
        when(jwt.getId()).thenReturn(null);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
        verify(revocationPort, never()).isRevoked(Instant.now().toString()); // never called
    }

    @Test
    void validate_nullJti_neverQueriesRevocationStore() {
        when(jwt.getId()).thenReturn(null);

        validator.validate(jwt);

        verify(revocationPort, never()).isRevoked(org.mockito.ArgumentMatchers.anyString());
    }
}
