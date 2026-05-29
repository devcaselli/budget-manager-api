package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.PasswordEncoderPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import br.com.casellisoftware.budgetmanager.domain.user.User;
import java.time.LocalDateTime;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.domain.user.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final String EMAIL    = "user@example.com";
    private static final String PASSWORD = "secret";
    private static final String HASH     = "$2a$12$somehashedvalue";
    private static final String USER_ID  = "user-1";

    private static final long REFRESH_EXP = 604800L;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private RefreshTokenPort refreshTokenPort;

    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(
                userRepository, passwordEncoder, tokenGenerator, refreshTokenPort, REFRESH_EXP);
    }

    private User user() {
        return new User(USER_ID, EMAIL, HASH, LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void execute_validCredentials_returnsToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
        when(tokenGenerator.generate(USER_ID, EMAIL))
                .thenReturn(new TokenOutput("tok", "Bearer", 900, null, 0));

        TokenOutput result = useCase.execute(new AuthInput(EMAIL, PASSWORD));

        assertThat(result.accessToken()).isEqualTo("tok");
        assertThat(result.refreshToken()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    // Timing-safe: dummy hash always consumed
    // -----------------------------------------------------------------------

    @Test
    void execute_emailNotFound_stillCallsPasswordEncoderWithDummyHash() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        // passwordEncoder.matches must be called with the dummy hash — not skipped
        when(passwordEncoder.matches(eq(PASSWORD), eq(AuthenticateUserUseCase.DUMMY_BCRYPT_HASH)))
                .thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthInput(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordEncoder).matches(PASSWORD, AuthenticateUserUseCase.DUMMY_BCRYPT_HASH);
        verify(tokenGenerator, never()).generate(any(), any());
    }

    @Test
    void execute_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthInput(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenGenerator, never()).generate(any(), any());
    }

    @Test
    void execute_emailNotFound_throwsInvalidCredentials_notUserNotFoundException() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        // Must throw InvalidCredentialsException — NOT UserNotFoundException —
        // so the handler cannot leak account existence via exception type.
        assertThatThrownBy(() -> useCase.execute(new AuthInput(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void execute_emailNotFound_passwordEncoderCalledExactlyOnce() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthInput(EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        // Exactly one BCrypt call — not zero (timing bypass) and not two (double work)
        verify(passwordEncoder).matches(any(), any());
    }
}
