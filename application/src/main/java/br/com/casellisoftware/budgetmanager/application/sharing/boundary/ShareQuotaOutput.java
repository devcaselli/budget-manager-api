package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import java.math.BigDecimal;
import java.util.List;

public record ShareQuotaOutput(
        String payerId,
        String payerName,
        BigDecimal ratio,
        BigDecimal amount,
        List<String> paymentIds
) {
}
