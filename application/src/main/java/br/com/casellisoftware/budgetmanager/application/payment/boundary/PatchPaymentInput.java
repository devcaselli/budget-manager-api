package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.math.BigDecimal;

/**
 * Input DTO for the patch-payment use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 */
public record PatchPaymentInput(
        String id,
        BigDecimal amount,
        String details
) {
}
