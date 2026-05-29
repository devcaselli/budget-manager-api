package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentWalletBalanceCalculatorTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void installmentTotal_walletNull_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> InstallmentWalletBalanceCalculator.installmentTotal(null, List.of()))
                .withMessage("wallet must not be null");
    }

    @Test
    void installmentTotal_installmentsNull_throwsNpe() {
        Wallet wallet = walletFor(YearMonth.of(2026, 6));

        assertThatNullPointerException()
                .isThrownBy(() -> InstallmentWalletBalanceCalculator.installmentTotal(wallet, null))
                .withMessage("installments must not be null");
    }

    @Test
    void installmentTotal_emptyList_returnsZero() {
        Wallet wallet = walletFor(YearMonth.of(2026, 6));

        Money total = InstallmentWalletBalanceCalculator.installmentTotal(wallet, List.of());

        assertThat(total.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void installmentTotal_sourceWallet_returnsZero() {
        Wallet sourceWallet = walletFor(YearMonth.of(2026, 5));
        Installment installment = installment(YearMonth.of(2026, 5));

        Money total = InstallmentWalletBalanceCalculator.installmentTotal(sourceWallet, List.of(installment));

        assertThat(total.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void installmentTotal_futureWalletWithinWindow_sumsInstallmentValue() {
        Wallet juneWallet = walletFor(YearMonth.of(2026, 6));
        Installment installment = installment(YearMonth.of(2026, 5));

        Money total = InstallmentWalletBalanceCalculator.installmentTotal(juneWallet, List.of(installment));

        assertThat(total.amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void installmentTotal_currencyMismatch_throws() {
        Wallet juneWallet = walletFor(YearMonth.of(2026, 6));
        Installment usdInstallment = Installment.create(
                "US thing",
                Money.of(new BigDecimal("600"), USD),
                Money.of(new BigDecimal("100"), USD),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w-other",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );

        assertThatThrownBy(() -> InstallmentWalletBalanceCalculator.installmentTotal(juneWallet, List.of(usdInstallment)))
                .isInstanceOf(WalletCurrencyMismatchException.class)
                .hasMessageContaining("BRL")
                .hasMessageContaining("USD");
    }

    private static Wallet walletFor(YearMonth month) {
        return new Wallet(
                "w-" + month,
                "wallet",
                Money.of(new BigDecimal("10000"), BRL),
                Money.of(new BigDecimal("10000"), BRL),
                LocalDate.of(2026, 1, 1),
                null,
                false,
                month,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    private static Installment installment(YearMonth sourceMonth) {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00"), BRL),
                Money.of(new BigDecimal("1000.00"), BRL),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w-source",
                sourceMonth,
                FlagEnum.NONE
        );
    }
}
