package br.com.casellisoftware.budgetmanager.domain.user.exception;

/**
 * Thrown when authentication fails — either because the email does not exist
 * or the password does not match.
 *
 * <p>A single exception type (rather than separate "user not found" vs "wrong password")
 * is intentional: it prevents timing-based account enumeration by ensuring the
 * caller cannot distinguish the two failure modes from the exception alone.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
