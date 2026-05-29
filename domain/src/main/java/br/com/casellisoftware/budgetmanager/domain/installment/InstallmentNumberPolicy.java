package br.com.casellisoftware.budgetmanager.domain.installment;

import java.math.BigDecimal;

public final class InstallmentNumberPolicy {

    public static final int MIN_INSTALLMENTS = 2;
    public static final int MAX_INSTALLMENTS = 120;

    /**
     * Max rounding error per installment under {@code Money.SCALE}/HALF_UP is
     * 0.005; budget 0.01 per installment to absorb operator-entered rounding too.
     */
    private static final BigDecimal PER_INSTALLMENT_ROUNDING_TOLERANCE = new BigDecimal("0.01");

    private InstallmentNumberPolicy() {
    }

    public static void validate(int installmentNumber) {
        if (installmentNumber < MIN_INSTALLMENTS) {
            throw new IllegalArgumentException(
                    "installmentNumber must be >= " + MIN_INSTALLMENTS + ", got " + installmentNumber);
        }
        if (installmentNumber > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException(
                    "installmentNumber must be <= " + MAX_INSTALLMENTS + ", got " + installmentNumber);
        }
    }

    public static BigDecimal roundingToleranceFor(int installmentNumber) {
        validate(installmentNumber);
        return BigDecimal.valueOf(installmentNumber).multiply(PER_INSTALLMENT_ROUNDING_TOLERANCE);
    }
}
