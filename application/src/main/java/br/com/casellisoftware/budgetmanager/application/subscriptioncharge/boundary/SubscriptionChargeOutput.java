package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionChargeOutput(
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
    public SubscriptionChargeOutput(String id,
                                    String subscriptionId,
                                    String walletId,
                                    YearMonth month,
                                    BigDecimal amount,
                                    BigDecimal remaining) {
        this(id, subscriptionId, walletId, month, amount, remaining, FlagEnum.NONE, false, null);
    }

    public SubscriptionChargeOutput(String id,
                                    String subscriptionId,
                                    String walletId,
                                    YearMonth month,
                                    BigDecimal amount,
                                    BigDecimal remaining,
                                    FlagEnum flag) {
        this(id, subscriptionId, walletId, month, amount, remaining, flag, false, null);
    }
}
