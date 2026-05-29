package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Email String email,
        @NotBlank
        @Size(min = 12, max = 128, message = "must be between 12 and 128 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                 message = "must contain at least one letter and one digit")
        String password
) {
}
