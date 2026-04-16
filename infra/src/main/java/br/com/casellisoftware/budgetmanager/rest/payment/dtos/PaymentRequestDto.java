package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequestDto(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotNull(message = "paymentDate is required")
        // Instant is the contract type end-to-end (DTO → domain → persistence); UTC avoids timezone drift across regions.
        @PastOrPresent(message = "paymentDate cannot be in the future")
        Instant paymentDate,

        String details
) {
}
