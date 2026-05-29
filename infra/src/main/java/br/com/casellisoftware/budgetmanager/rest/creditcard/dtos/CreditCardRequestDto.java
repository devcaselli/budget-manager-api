package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreditCardRequestDto(

        @NotBlank
        @Size(max = 80)
        String name
) {
}
