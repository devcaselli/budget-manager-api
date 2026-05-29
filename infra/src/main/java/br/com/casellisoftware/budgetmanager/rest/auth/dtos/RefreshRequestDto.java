package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(@NotBlank String refreshToken) {
}
