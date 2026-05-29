package br.com.casellisoftware.budgetmanager.application.auth.usecase;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.PasswordEncoderPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.UserOutput;
import br.com.casellisoftware.budgetmanager.domain.user.User;
import br.com.casellisoftware.budgetmanager.domain.user.UserRepository;
import br.com.casellisoftware.budgetmanager.domain.user.exception.UserAlreadyExistsException;

/**
 * Registers a new user.
 *
 * <h2>Timing-safe design</h2>
 * <p>BCrypt hashing takes ~200 ms. Without countermeasures, an attacker can distinguish
 * "email already registered" (fast path — no hashing) from "registration succeeded"
 * (slow path — hashing) via response latency, enabling account enumeration.
 * To close this side-channel, a dummy {@code encode} call is always made on the
 * "already exists" path so both branches take comparable time.</p>
 */
public class RegisterUserUseCase implements RegisterUserBoundary {

    private final UserRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserOutput execute(RegisterUserInput input) {
        if (userRepository.existsByEmail(input.email())) {
            // Always hash — equalizes response time with the success path and
            // prevents account enumeration via timing differences.
            passwordEncoder.encode(input.rawPassword());
            throw new UserAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(input.rawPassword());
        User user = User.create(input.email(), passwordHash);
        User saved = userRepository.save(user);

        return new UserOutput(saved.getId(), saved.getEmail(), saved.getCreatedAt());
    }
}
