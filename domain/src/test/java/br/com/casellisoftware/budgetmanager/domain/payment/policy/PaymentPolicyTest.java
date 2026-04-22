package br.com.casellisoftware.budgetmanager.domain.payment.policy;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.payment.AmountExceedsRemainingException;
import br.com.casellisoftware.budgetmanager.domain.payment.CurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.payment.WalletMismatchException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPolicyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Money ONE_HUNDRED = Money.of("100.00");
    private static final Money FIFTY = Money.of("50.00");

    @Test
    void validate_acceptsMatchingWalletCurrencyAndAvailableRemaining() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-1", ONE_HUNDRED, ONE_HUNDRED);

        assertThatCode(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsBulletFromDifferentWallet() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-2", ONE_HUNDRED, ONE_HUNDRED);

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-1"))
                .isInstanceOf(WalletMismatchException.class)
                .hasMessageContaining("Expense wallet wallet-1 does not match bullet wallet wallet-2");
    }

    @Test
    void validate_rejectsInputWalletDifferentFromExpenseWallet() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-1", ONE_HUNDRED, ONE_HUNDRED);

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-2"))
                .isInstanceOf(WalletMismatchException.class)
                .hasMessageContaining("Input wallet wallet-2 does not match expense wallet wallet-1");
    }

    @Test
    void validate_rejectsCurrencyMismatchBetweenExpenseAndBullet() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-1", Money.of(BigDecimal.valueOf(100), USD), Money.of(BigDecimal.valueOf(100), USD));

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-1"))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void validate_rejectsCurrencyMismatchBetweenExpenseAndPayment() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Money usdPayment = Money.of(BigDecimal.valueOf(50), USD);

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, usdPayment, "wallet-1"))
                .isInstanceOf(CurrencyMismatchException.class)
                .hasMessageContaining("payment=USD");
    }

    @Test
    void validate_rejectsPaymentGreaterThanExpenseRemaining() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, Money.of("40.00"));
        Bullet bullet = bullet("wallet-1", ONE_HUNDRED, ONE_HUNDRED);

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-1"))
                .isInstanceOf(AmountExceedsRemainingException.class)
                .hasMessageContaining("expense remaining");
    }

    @Test
    void validate_rejectsPaymentGreaterThanBulletRemaining() {
        Expense expense = expense("wallet-1", ONE_HUNDRED, ONE_HUNDRED);
        Bullet bullet = bullet("wallet-1", ONE_HUNDRED, Money.of("40.00"));

        assertThatThrownBy(() -> PaymentPolicy.validate(expense, bullet, FIFTY, "wallet-1"))
                .isInstanceOf(AmountExceedsRemainingException.class)
                .hasMessageContaining("bullet remaining");
    }

    private static Expense expense(String walletId, Money cost, Money remaining) {
        return new Expense("expense-1", walletId, "Lunch", cost, remaining, LocalDate.now());
    }

    private static Bullet bullet(String walletId, Money budget, Money remaining) {
        return Bullet.rebuild("bullet-1", "Food", budget, remaining, walletId);
    }
}
