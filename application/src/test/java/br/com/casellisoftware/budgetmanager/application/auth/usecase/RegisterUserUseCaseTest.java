package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.PasswordEncoderPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.UserOutput;
import br.com.casellisoftware.budgetmanager.domain.user.User;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.domain.user.exception.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    private static final String EMAIL    = "user@example.com";
    private static final String PASSWORD = "secret123";
    private static final String HASH     = "$2a$12$hashedvalue";

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoderPort passwordEncoder;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordEncoder);
    }

    // -----------------------------------------------------------------------
    // Happy path
    // -----------------------------------------------------------------------

    @Test
    void execute_newEmail_createsAndReturnsUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        User saved = User.create(EMAIL, HASH);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserOutput result = useCase.execute(new RegisterUserInput(EMAIL, PASSWORD));

        assertThat(result.email()).isEqualTo(EMAIL);
        verify(userRepository).save(any(User.class));
    }

    // -----------------------------------------------------------------------
    // Account enumeration prevention
    // -----------------------------------------------------------------------

    @Test
    void execute_existingEmail_throwsUserAlreadyExists_notRevealingEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThatThrownBy(() -> useCase.execute(new RegisterUserInput(EMAIL, PASSWORD)))
                .isInstanceOf(UserAlreadyExistsException.class)
                // Message must NOT contain the email — prevents enumeration via exception
                .hasMessageNotContaining(EMAIL)
                .hasMessage("Registration failed");
    }

    @Test
    void execute_existingEmail_stillCallsPasswordEncoderForTimingParity() {
        // BCrypt takes ~200ms. Skipping encode() on the "already exists" path
        // would allow enumeration via response latency. Encoder must always run.
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThatThrownBy(() -> useCase.execute(new RegisterUserInput(EMAIL, PASSWORD)))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void execute_existingEmail_neverPersistsUser() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn(HASH);

        assertThatThrownBy(() -> useCase.execute(new RegisterUserInput(EMAIL, PASSWORD)))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void execute_existingEmail_passwordEncoderCalledExactlyOnce() {
        // Exactly one encode call — not zero (timing bypass) and not two (double work)
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        assertThatThrownBy(() -> useCase.execute(new RegisterUserInput(EMAIL, PASSWORD)))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(passwordEncoder).encode(PASSWORD);
    }

    // -----------------------------------------------------------------------
    // New user — encoder called exactly once
    // -----------------------------------------------------------------------

    @Test
    void execute_newEmail_passwordEncoderCalledExactlyOnce() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(userRepository.save(any(User.class))).thenReturn(User.create(EMAIL, HASH));

        useCase.execute(new RegisterUserInput(EMAIL, PASSWORD));

        verify(passwordEncoder).encode(PASSWORD);
    }
}
