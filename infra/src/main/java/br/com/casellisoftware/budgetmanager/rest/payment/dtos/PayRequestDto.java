package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
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
        String expenseId,
        FlagEnum flag
) {
    public PayRequestDto(PaymentRequestDto payment, String bulletId, String expenseId) {
        this(payment, bulletId, expenseId, FlagEnum.NONE);
    }
}
