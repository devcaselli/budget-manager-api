package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenGeneratorTest {

    private static final String ISSUER   = "test-issuer";
    private static final long   EXP_SECS = 3600L;
    private static final String USER_ID  = "user-123";
    private static final String EMAIL    = "user@example.com";

    @Mock private JwtEncoder jwtEncoder;

    private JwtTokenGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new JwtTokenGenerator(jwtEncoder, ISSUER, EXP_SECS);
    }

    @Test
    void generate_includesJtiClaimAsUuid() {
        stubEncoder("signed-token");
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);

        generator.generate(USER_ID, EMAIL);

        verify(jwtEncoder).encode(captor.capture());
        String jti = captor.getValue().getClaims().getId();
        assertThat(jti).isNotNull();
        assertThat(UUID.fromString(jti)).isInstanceOf(UUID.class); // valid UUID — no exception
    }

    @Test
    void generate_jtiIsUniquePerCall() {
        stubEncoder("token-1");
        ArgumentCaptor<JwtEncoderParameters> captor1 = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        generator.generate(USER_ID, EMAIL);
        verify(jwtEncoder).encode(captor1.capture());
        String jti1 = captor1.getValue().getClaims().getId();

        stubEncoder("token-2");
        ArgumentCaptor<JwtEncoderParameters> captor2 = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        generator.generate(USER_ID, EMAIL);
        verify(jwtEncoder, org.mockito.Mockito.times(2)).encode(captor2.capture());
        String jti2 = captor2.getValue().getClaims().getId();

        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void generate_returnsCorrectTokenOutput() {
        stubEncoder("signed-token");

        TokenOutput output = generator.generate(USER_ID, EMAIL);

        assertThat(output.accessToken()).isEqualTo("signed-token");
        assertThat(output.tokenType()).isEqualTo("Bearer");
        assertThat(output.expiresIn()).isEqualTo(EXP_SECS);
    }

    private void stubEncoder(String tokenValue) {
        Jwt stubJwt = new Jwt(
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(EXP_SECS),
                Map.of("alg", "RS256"),
                Map.of("sub", USER_ID)
        );
        when(jwtEncoder.encode(any())).thenReturn(stubJwt);
    }
}
