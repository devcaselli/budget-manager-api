package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import java.time.YearMonth;
import java.util.List;

public record SubscriptionResponseDto(
        String id,
        String description,
        String currency,
        String state,
        YearMonth startMonth,
        YearMonth endMonth,
        List<SubscriptionVersionResponseDto> versions,
        FlagEnum flag,
        String creditCardId
) {
    public SubscriptionResponseDto(String id,
                                   String description,
                                   String currency,
                                   YearMonth startMonth,
                                   YearMonth endMonth,
                                   List<SubscriptionVersionResponseDto> versions) {
        this(id, description, currency, "PRODUCTION", startMonth, endMonth, versions, FlagEnum.NONE, null);
    }

    public SubscriptionResponseDto(String id,
                                   String description,
                                   String currency,
                                   String state,
                                   YearMonth startMonth,
                                   YearMonth endMonth,
                                   List<SubscriptionVersionResponseDto> versions) {
        this(id, description, currency, state, startMonth, endMonth, versions, FlagEnum.NONE, null);
    }

    public SubscriptionResponseDto(String id,
                                   String description,
                                   String currency,
                                   String state,
                                   YearMonth startMonth,
                                   YearMonth endMonth,
                                   List<SubscriptionVersionResponseDto> versions,
                                   FlagEnum flag) {
        this(id, description, currency, state, startMonth, endMonth, versions, flag, null);
    }
}
