package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Factory Method: builds an {@link Installment} from a saved {@link Expense}.
 */
public final class InstallmentFactory {

    private InstallmentFactory() {
    }

    public static Installment fromExpense(Expense expense,
                                          int installmentNumber,
                                          YearMonth sourceEffectiveMonth,
                                          FlagEnum flag) {
        return fromExpense(expense, installmentNumber, sourceEffectiveMonth, flag, Clock.systemDefaultZone());
    }

    public static Installment fromExpense(Expense expense,
                                          int installmentNumber,
                                          YearMonth sourceEffectiveMonth,
                                          FlagEnum flag,
                                          Clock clock) {
        Objects.requireNonNull(expense, "expense must not be null");
        Objects.requireNonNull(sourceEffectiveMonth, "sourceEffectiveMonth must not be null");
        Objects.requireNonNull(clock, "clock must not be null");
        InstallmentNumberPolicy.validate(installmentNumber);

        Money original = expense.getCost();
        BigDecimal perInstallmentAmount = original.amount()
                .divide(BigDecimal.valueOf(installmentNumber), Money.SCALE, Money.ROUNDING);
        Money perInstallment = Money.of(perInstallmentAmount, original.currency());

        return Installment.create(
                expense.getName(),
                original,
                perInstallment,
                installmentNumber,
                expense.getPurchaseDate(),
                expense.getCreditCardId(),
                expense.getId(),
                expense.getWalletId(),
                sourceEffectiveMonth,
                flag,
                clock,
                expense.getOwnerId()
        );
    }
}
