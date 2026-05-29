package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InstallmentOutputAssembler {

    private static final int DISPLAY_SCALE = 2;

    private InstallmentOutputAssembler() {
    }

    public static InstallmentOutput from(Installment installment) {
        return from(installment, null);
    }

    /**
     * Builds the output with optional share-awareness.
     *
     * <p>If {@code activeShare} is non-null, {@code effectiveOriginalValue} and
     * {@code effectiveInstallmentValue} are computed as
     * {@code value * ownerRatio} (rounded HALF_EVEN to 2 decimals for display)
     * so the front end shows only the owner's portion. The raw values
     * (originalValue / installmentValue) remain untouched for accounting.</p>
     */
    public static InstallmentOutput from(Installment installment, Share activeShare) {
        BigDecimal originalValue = installment.getOriginalValue().amount();
        BigDecimal installmentValue = installment.getInstallmentValue().amount();
        boolean shared = activeShare != null;
        BigDecimal ownerRatio = shared ? activeShare.getOwnerRatio() : null;
        BigDecimal effectiveOriginalValue = shared
                ? originalValue.multiply(ownerRatio).setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN)
                : originalValue;
        BigDecimal effectiveInstallmentValue = shared
                ? installmentValue.multiply(ownerRatio).setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN)
                : installmentValue;
        return new InstallmentOutput(
                installment.getId(),
                installment.getDescription(),
                installment.getDetails(),
                originalValue,
                installmentValue,
                installment.getOriginalValue().currency().getCurrencyCode(),
                installment.getInstallmentNumber(),
                installment.getPurchaseDate(),
                installment.getLastInstallmentDate(),
                installment.getCreditCardId(),
                installment.getSourceWalletId(),
                installment.getSourceEffectiveMonth(),
                installment.isDeleted(),
                installment.getDeletedAt(),
                installment.getFlag(),
                shared,
                ownerRatio,
                effectiveOriginalValue,
                effectiveInstallmentValue
        );
    }
}
