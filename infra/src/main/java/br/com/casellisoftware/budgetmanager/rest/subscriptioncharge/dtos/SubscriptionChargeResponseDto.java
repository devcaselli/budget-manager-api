package br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionChargeResponseDto(
        String id,
        String subscriptionId,
        String walletId,
        YearMonth month,
        BigDecimal amount,
        BigDecimal remaining,
        FlagEnum flag,
        boolean shared,
        BigDecimal effectiveOwnerAmount
) {
    public SubscriptionChargeResponseDto(String id,
                                         String subscriptionId,
                                         String walletId,
                                         YearMonth month,
                                         BigDecimal amount,
                                         BigDecimal remaining) {
        this(id, subscriptionId, walletId, month, amount, remaining, FlagEnum.NONE, false, null);
    }

    public SubscriptionChargeResponseDto(String id,
                                         String subscriptionId,
                                         String walletId,
                                         YearMonth month,
                                         BigDecimal amount,
                                         BigDecimal remaining,
                                         FlagEnum flag) {
        this(id, subscriptionId, walletId, month, amount, remaining, flag, false, null);
    }
}
