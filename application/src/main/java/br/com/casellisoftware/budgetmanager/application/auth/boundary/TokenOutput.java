package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public record TokenOutput(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
}
