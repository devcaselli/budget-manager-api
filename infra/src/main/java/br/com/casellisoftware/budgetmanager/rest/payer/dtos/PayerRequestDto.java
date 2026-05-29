package br.com.casellisoftware.budgetmanager.rest.payer.dtos;

import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PayerRequestDto(
        @NotBlank @Size(max = 120) String name,
        @NotNull PayerType type,
        String walletId,
        String subscriptionId,
        @NotNull LocalDate paymentDate
) {
    @AssertTrue(message = "walletId must be null for STANDING and non-blank for TRANSIENT")
    public boolean isLifecycleValid() {
        if (type == null) {
            return true;
        }
        if (type == PayerType.STANDING) {
            return walletId == null;
        }
        return walletId != null && !walletId.isBlank();
    }
}
