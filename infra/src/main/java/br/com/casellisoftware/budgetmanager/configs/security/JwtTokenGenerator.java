package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class JwtTokenGenerator implements TokenGeneratorPort {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenGenerator(JwtEncoder jwtEncoder,
                             @Value("${app.jwt.issuer:budgetmanager-api}") String issuer,
                             @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    @Override
    public TokenOutput generate(String userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .audience(List.of(issuer))
                .build();

        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // refreshToken and refreshExpiresIn are populated by the use case layer,
        // not here — this generator is responsible only for the JWT access token.
        return new TokenOutput(tokenValue, "Bearer", expirationSeconds, null, 0);
    }
}
