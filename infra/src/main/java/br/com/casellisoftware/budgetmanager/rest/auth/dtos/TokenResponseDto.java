package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

public record TokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {
}
