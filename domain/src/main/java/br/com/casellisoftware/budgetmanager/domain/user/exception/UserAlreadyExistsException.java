package br.com.casellisoftware.budgetmanager.domain.user.exception;

/**
 * Thrown when a registration attempt is made for an email that is already registered.
 *
 * <p>The message is intentionally generic — it must NOT include the email address or
 * any hint that distinguishes "email taken" from "registration succeeded". This prevents
 * account enumeration: callers should return the same opaque response regardless of
 * whether this exception was thrown.</p>
 */
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException() {
        super("Registration failed");
    }
}
