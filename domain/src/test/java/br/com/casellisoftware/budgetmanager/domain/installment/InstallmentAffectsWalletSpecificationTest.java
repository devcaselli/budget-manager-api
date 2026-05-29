package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentAffectsWalletSpecificationTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void sourceWallet_isNotAffected() {
        Installment installment = sampleInstallment(YearMonth.of(2026, 5));
        Wallet sourceWallet = walletFor(YearMonth.of(2026, 5));

        assertThat(InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, sourceWallet)).isFalse();
    }

    @Test
    void futureWalletWithinWindow_isAffected() {
        Installment installment = sampleInstallment(YearMonth.of(2026, 5));
        Wallet juneWallet = walletFor(YearMonth.of(2026, 6));

        assertThat(InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, juneWallet)).isTrue();
    }

    @Test
    void walletEqualToLastInstallmentMonth_isAffected() {
        Installment installment = sampleInstallment(YearMonth.of(2026, 5));
        Wallet octoberWallet = walletFor(YearMonth.of(2026, 10));

        assertThat(InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, octoberWallet)).isTrue();
    }

    @Test
    void walletAfterLastInstallmentMonth_isNotAffected() {
        Installment installment = sampleInstallment(YearMonth.of(2026, 5));
        Wallet novemberWallet = walletFor(YearMonth.of(2026, 11));

        assertThat(InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, novemberWallet)).isFalse();
    }

    @Test
    void deletedInstallment_neverAffects() {
        Installment installment = sampleInstallment(YearMonth.of(2026, 5)).delete(FIXED_CLOCK);
        Wallet juneWallet = walletFor(YearMonth.of(2026, 6));

        assertThat(InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, juneWallet)).isFalse();
    }

    private static Installment sampleInstallment(YearMonth sourceMonth) {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                sourceMonth,
                FlagEnum.NONE
        );
    }

    private static Wallet walletFor(YearMonth month) {
        return new Wallet(
                "w-" + month,
                "wallet",
                Money.of(new BigDecimal("1000")),
                Money.of(new BigDecimal("1000")),
                LocalDate.of(2026, 1, 1),
                null,
                false,
                month,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }
}
