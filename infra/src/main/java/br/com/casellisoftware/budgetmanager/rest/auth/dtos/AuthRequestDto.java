package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequestDto(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
