package br.com.casellisoftware.budgetmanager.domain.wallet.policy;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulletAllocationPolicyTest {

    @Test
    void validateAllocation_whenBudgetEqualsRemaining_doesNotThrow() {
        Wallet wallet = wallet("100.00", "40.00");

        assertThatCode(() -> BulletAllocationPolicy.validateAllocation(wallet, Money.of("40.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAllocation_whenBudgetExceedsRemaining_throws() {
        Wallet wallet = wallet("100.00", "40.00");

        assertThatThrownBy(() -> BulletAllocationPolicy.validateAllocation(wallet, Money.of("40.01")))
                .isInstanceOf(WalletAllocationExceededException.class)
                .hasMessageContaining("exceeds wallet remaining");
    }

    @Test
    void validateAllocation_whenCurrencyDiffers_throwsSemanticException() {
        Wallet wallet = wallet("100.00", "40.00");
        Money usdBudget = Money.of(new BigDecimal("10.00"), Currency.getInstance("USD"));

        assertThatThrownBy(() -> BulletAllocationPolicy.validateAllocation(wallet, usdBudget))
                .isInstanceOf(WalletCurrencyMismatchException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void validateReallocation_whenIncreaseFits_doesNotThrow() {
        Wallet wallet = wallet("100.00", "40.00");

        assertThatCode(() -> BulletAllocationPolicy.validateReallocation(
                wallet, Money.of("50.00"), Money.of("80.00"), Money.of("20.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateReallocation_whenIncreaseExceedsRemaining_throws() {
        Wallet wallet = wallet("100.00", "20.00");

        assertThatThrownBy(() -> BulletAllocationPolicy.validateReallocation(
                wallet, Money.of("50.00"), Money.of("80.00"), Money.of("20.00")))
                .isInstanceOf(WalletAllocationExceededException.class)
                .hasMessageContaining("exceeds wallet remaining");
    }

    @Test
    void validateReallocation_whenReductionKeepsConsumedAmount_doesNotThrow() {
        Wallet wallet = wallet("100.00", "20.00");

        assertThatCode(() -> BulletAllocationPolicy.validateReallocation(
                wallet, Money.of("50.00"), Money.of("35.00"), Money.of("20.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void validateReallocation_whenReductionCutsAlreadyConsumedAmount_throws() {
        Wallet wallet = wallet("100.00", "20.00");

        assertThatThrownBy(() -> BulletAllocationPolicy.validateReallocation(
                wallet, Money.of("50.00"), Money.of("29.99"), Money.of("20.00")))
                .isInstanceOf(WalletAllocationExceededException.class)
                .hasMessageContaining("already consumed");
    }

    private static Wallet wallet(String budget, String remaining) {
        return new Wallet("wallet-1", "wallet", Money.of(budget), Money.of(remaining), null, null, false);
    }
}
