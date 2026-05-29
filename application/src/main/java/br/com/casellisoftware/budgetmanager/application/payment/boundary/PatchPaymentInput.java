package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;

/**
 * Input DTO for the patch-payment use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 */
public record PatchPaymentInput(
        String id,
        BigDecimal amount,
        String details,
        FlagEnum flag,
        String ownerId
) {
    public PatchPaymentInput(String id, BigDecimal amount, String details, FlagEnum flag) {
        this(id, amount, details, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchPaymentInput(String id, BigDecimal amount, String details) {
        this(id, amount, details, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchPaymentInput withOwnerId(String ownerId) {
        return new PatchPaymentInput(id, amount, details, flag, ownerId);
    }
}
