package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import jakarta.validation.constraints.Size;

import java.util.List;

public record CreditCardPatchRequestDto(

        @Size(max = 20, message = "labels must not exceed 20 entries")
        List<@Size(max = 80) String> labels
) {
}
