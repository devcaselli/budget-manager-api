package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthenticateUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.PasswordEncoderPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import br.com.casellisoftware.budgetmanager.domain.user.User;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.domain.user.exception.InvalidCredentialsException;

import java.time.Instant;
import java.util.UUID;

/**
 * Authenticates a user by email and password.
 *
 * <h2>Timing-safe design</h2>
 * <p>BCrypt hashing takes ~200ms. Without countermeasures, an attacker can enumerate
 * registered accounts by measuring response latency: a missing user returns immediately
 * while a wrong password waits for BCrypt. To close this side-channel:</p>
 * <ol>
 *   <li>Look up the user but do NOT fail immediately if absent.</li>
 *   <li>Always call {@link PasswordEncoderPort#matches} — with the real hash when the
 *       user exists, or with {@link #DUMMY_BCRYPT_HASH} otherwise.</li>
 *   <li>Only after {@code matches} returns do we decide to throw
 *       {@link InvalidCredentialsException} — a single, generic exception that gives
 *       no hint about which condition failed.</li>
 * </ol>
 *
 * <p>The dummy hash was generated from a random password at build time and is safe to
 * embed in source: it is never a valid credential and its only purpose is to force
 * BCrypt to run its full cost computation on the "user not found" path.</p>
 */
public class AuthenticateUserUseCase implements AuthenticateUserBoundary {

    /**
     * Pre-computed BCrypt hash (cost 12) used when the email is not found,
     * so the response time matches the "wrong password" path.
     *
     * <p>Generated with: {@code BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12))}</p>
     */
    static final String DUMMY_BCRYPT_HASH =
            "$2a$12$KIx6VzmWCMNMPCONmWZbNuLPcGxHEOJMhGSEGNqL9oqHzq7r0YO2W";

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenGeneratorPort tokenGenerator;
    private final RefreshTokenPort refreshTokenPort;
    private final long refreshExpirationSeconds;

    public AuthenticateUserUseCase(UserRepository userRepository,
                                   PasswordEncoderPort passwordEncoder,
                                   TokenGeneratorPort tokenGenerator,
                                   RefreshTokenPort refreshTokenPort,
                                   long refreshExpirationSeconds) {
        this.userRepository          = userRepository;
        this.passwordEncoder         = passwordEncoder;
        this.tokenGenerator          = tokenGenerator;
        this.refreshTokenPort        = refreshTokenPort;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    @Override
    public TokenOutput execute(AuthInput input) {
        User user = userRepository.findByEmail(input.email()).orElse(null);

        // Always run BCrypt — real hash when user exists, dummy hash otherwise.
        // This equalizes response time regardless of whether the email is registered,
        // preventing account enumeration via timing differences.
        String hashToCheck = user != null ? user.getPasswordHash() : DUMMY_BCRYPT_HASH;
        boolean matches = passwordEncoder.matches(input.rawPassword(), hashToCheck);

        if (user == null || !matches) {
            throw new InvalidCredentialsException();
        }

        TokenOutput accessTokenOutput = tokenGenerator.generate(user.getId(), user.getEmail());

        String refreshToken = UUID.randomUUID().toString();
        Instant refreshExpiry = Instant.now().plusSeconds(refreshExpirationSeconds);
        refreshTokenPort.save(refreshToken, user.getId(), user.getEmail(), refreshExpiry);

        return new TokenOutput(
                accessTokenOutput.accessToken(),
                accessTokenOutput.tokenType(),
                accessTokenOutput.expiresIn(),
                refreshToken,
                refreshExpirationSeconds
        );
    }
}
