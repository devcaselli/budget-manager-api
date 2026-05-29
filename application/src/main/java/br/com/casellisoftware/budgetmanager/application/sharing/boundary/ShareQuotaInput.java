package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import java.math.BigDecimal;

public record ShareQuotaInput(
        String payerId,
        TransientPayerSpec transient_,
        BigDecimal amount
) {
}
