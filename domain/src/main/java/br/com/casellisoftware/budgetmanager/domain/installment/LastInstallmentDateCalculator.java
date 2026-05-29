package br.com.casellisoftware.budgetmanager.domain.installment;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Computes the {@code YearMonth} of the last installment.
 *
 * <p>Decision (see implementation plan §0): {@code sourceEffectiveMonth.plusMonths(installmentNumber - 1)}.
 * If the purchase is in May and the user picks 6 installments, the last
 * installment lands in October. May is parcel 1; October is parcel 6.</p>
 */
public final class LastInstallmentDateCalculator {

    private LastInstallmentDateCalculator() {
    }

    public static YearMonth calculate(LocalDate purchaseDate, int installmentNumber) {
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        validateInstallmentNumber(installmentNumber);
        return YearMonth.from(purchaseDate).plusMonths(installmentNumber - 1L);
    }

    public static YearMonth calculate(YearMonth sourceEffectiveMonth, int installmentNumber) {
        Objects.requireNonNull(sourceEffectiveMonth, "sourceEffectiveMonth must not be null");
        validateInstallmentNumber(installmentNumber);
        return sourceEffectiveMonth.plusMonths(installmentNumber - 1L);
    }

    private static void validateInstallmentNumber(int installmentNumber) {
        InstallmentNumberPolicy.validate(installmentNumber);
    }
}
