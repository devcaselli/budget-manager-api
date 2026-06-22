package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservedBudgetWalletBalanceCalculatorTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final YearMonth MARCH = YearMonth.of(2025, 3);
    private static final YearMonth AUGUST = YearMonth.of(2025, 8);

    private static Wallet wallet(YearMonth month, Currency currency) {
        return new Wallet(
                "wallet-1", "March wallet",
                Money.of(java.math.BigDecimal.valueOf(5000), currency), Money.of(java.math.BigDecimal.valueOf(5000), currency),
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 4, 1), false,
                month,
                WalletState.PRODUCTION, FlagEnum.NONE
        );
    }

    private static ReservedBudget reservedBudget(String description, String amount, YearMonth startMonth) {
        return ReservedBudget.create(description, null, BRL, Money.of(amount, BRL), startMonth, FlagEnum.NONE, "owner-1");
    }

    @Test
    void reservedBudgetTotal_emptyList_returnsZero() {
        Money total = ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet(MARCH, BRL), List.of());
        assertThat(total).isEqualTo(Money.of("0.00", BRL));
    }

    @Test
    void reservedBudgetTotal_sumsEffectiveAmountForWalletMonth() {
        ReservedBudget aluguel = reservedBudget("Aluguel", "2000.00", MARCH);
        ReservedBudget mercado = reservedBudget("Mercado", "800.00", MARCH);

        Money total = ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(
                wallet(MARCH, BRL), List.of(aluguel, mercado));

        assertThat(total).isEqualTo(Money.of("2800.00", BRL));
    }

    @Test
    void reservedBudgetTotal_usesVersionEffectiveForWalletMonth() {
        // 2000 from March, 1500 from August. A March wallet sees 2000; an August wallet sees 1500.
        ReservedBudget aluguel = reservedBudget("Aluguel", "2000.00", MARCH)
                .addVersion(AUGUST, Money.of("1500.00", BRL));

        assertThat(ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet(MARCH, BRL), List.of(aluguel)))
                .isEqualTo(Money.of("2000.00", BRL));
        assertThat(ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet(AUGUST, BRL), List.of(aluguel)))
                .isEqualTo(Money.of("1500.00", BRL));
    }

    @Test
    void reservedBudgetTotal_currencyMismatch_throws() {
        ReservedBudget aluguel = reservedBudget("Aluguel", "2000.00", MARCH);
        assertThatThrownBy(() -> ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet(MARCH, USD), List.of(aluguel)))
                .isInstanceOf(WalletCurrencyMismatchException.class);
    }
}
