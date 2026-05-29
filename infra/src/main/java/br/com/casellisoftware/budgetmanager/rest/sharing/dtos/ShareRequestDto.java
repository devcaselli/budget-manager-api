package br.com.casellisoftware.budgetmanager.rest.sharing.dtos;

import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.rest.validation.IsoCurrencyCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ShareRequestDto(
        @NotBlank String walletId,
        @NotNull ShareSourceType sourceType,
        @NotBlank String sourceId,
        @NotNull @Positive BigDecimal totalAmount,
        @NotBlank @IsoCurrencyCode String currency,
        @NotNull @PositiveOrZero BigDecimal ownerShare,
        @NotNull @Size(min = 1) List<@Valid ShareQuotaRequestDto> quotas
) {
    @AssertTrue(message = "ownerShare + sum(quotas) must equal totalAmount")
    public boolean isAmountConsistent() {
        BigDecimal sum = quotas.stream()
                .map(ShareQuotaRequestDto::amount)
                .reduce(ownerShare, BigDecimal::add);
        return sum.subtract(totalAmount).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }
}
