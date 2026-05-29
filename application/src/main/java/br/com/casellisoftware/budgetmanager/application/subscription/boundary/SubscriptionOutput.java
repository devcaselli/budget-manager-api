package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.time.YearMonth;
import java.util.List;

public record SubscriptionOutput(
        String id,
        String description,
        String currency,
        String state,
        YearMonth startMonth,
        YearMonth endMonth,
        List<SubscriptionVersionOutput> versions,
        FlagEnum flag,
        String creditCardId
) {
    public SubscriptionOutput(String id,
                              String description,
                              String currency,
                              YearMonth startMonth,
                              YearMonth endMonth,
                              List<SubscriptionVersionOutput> versions) {
        this(id, description, currency, "PRODUCTION", startMonth, endMonth, versions, FlagEnum.NONE, null);
    }

    public SubscriptionOutput(String id,
                              String description,
                              String currency,
                              String state,
                              YearMonth startMonth,
                              YearMonth endMonth,
                              List<SubscriptionVersionOutput> versions) {
        this(id, description, currency, state, startMonth, endMonth, versions, FlagEnum.NONE, null);
    }

    public SubscriptionOutput(String id,
                              String description,
                              String currency,
                              String state,
                              YearMonth startMonth,
                              YearMonth endMonth,
                              List<SubscriptionVersionOutput> versions,
                              FlagEnum flag) {
        this(id, description, currency, state, startMonth, endMonth, versions, flag, null);
    }
}
