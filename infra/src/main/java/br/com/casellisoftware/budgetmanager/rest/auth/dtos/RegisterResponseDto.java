package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

/**
 * Generic registration response returned for both new and duplicate registrations.
 *
 * <p>The message is intentionally vague — it does not confirm whether the account
 * was created or already existed, preventing account enumeration (OWASP ASVS V3.2.2).</p>
 */
public record RegisterResponseDto(String message) {
}
