package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayRequestDto(
        @NotNull(message = "payment is required")
        @Valid
        PaymentRequestDto payment,

        @NotBlank(message = "bulletId is required")
        String bulletId,

        @NotBlank(message = "expenseId is required")
        String expenseId
) {
}
