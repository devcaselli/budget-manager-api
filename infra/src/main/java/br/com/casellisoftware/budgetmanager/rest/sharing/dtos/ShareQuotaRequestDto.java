package br.com.casellisoftware.budgetmanager.rest.sharing.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ShareQuotaRequestDto(
        String payerId,
        @Valid TransientPayerRequestDto transient_,
        @NotNull @Positive BigDecimal amount
) {
    @AssertTrue(message = "exactly one of payerId or transient_ must be set")
    public boolean isPayerRefValid() {
        return (payerId != null && transient_ == null) || (payerId == null && transient_ != null);
    }
}
