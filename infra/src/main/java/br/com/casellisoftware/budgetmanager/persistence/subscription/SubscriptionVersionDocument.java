package br.com.casellisoftware.budgetmanager.persistence.subscription;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubscriptionVersionDocument {

    private YearMonth effectiveMonth;
    private BigDecimal amount;
}
