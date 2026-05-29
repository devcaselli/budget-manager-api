package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenData;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import br.com.casellisoftware.budgetmanager.domain.user.exception.InvalidCredentialsException;

import java.time.Instant;
import java.util.UUID;

/**
 * Exchanges a valid refresh token for a new access + refresh token pair.
 *
 * <h2>Rotation</h2>
 * <p>The supplied refresh token is deleted before the new one is persisted.
 * Any reuse of the consumed token results in {@link InvalidCredentialsException},
 * which signals a potential theft and forces the client to re-authenticate.</p>
 */
public class RefreshUseCase implements RefreshBoundary {

    private final RefreshTokenPort refreshTokenPort;
    private final TokenGeneratorPort tokenGenerator;
    private final long refreshExpirationSeconds;

    public RefreshUseCase(RefreshTokenPort refreshTokenPort,
                          TokenGeneratorPort tokenGenerator,
                          long refreshExpirationSeconds) {
        this.refreshTokenPort       = refreshTokenPort;
        this.tokenGenerator         = tokenGenerator;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    public TokenOutput execute(RefreshTokenInput input) {
        RefreshTokenData data = refreshTokenPort.findByToken(input.refreshToken())
                .orElseThrow(InvalidCredentialsException::new);

        // Rotate: consume old token before issuing new one.
        // If this delete succeeds but the response never reaches the client,
        // the user must re-login — acceptable trade-off over allowing reuse.
        refreshTokenPort.delete(input.refreshToken());

        TokenOutput accessTokenOutput = tokenGenerator.generate(data.userId(), data.email());

        String newRefreshToken = UUID.randomUUID().toString();
        Instant newRefreshExpiry = Instant.now().plusSeconds(refreshExpirationSeconds);
        refreshTokenPort.save(newRefreshToken, data.userId(), data.email(), newRefreshExpiry);

        return new TokenOutput(
                accessTokenOutput.accessToken(),
                accessTokenOutput.tokenType(),
                accessTokenOutput.expiresIn(),
                newRefreshToken,
                refreshExpirationSeconds
        );
    }
}
