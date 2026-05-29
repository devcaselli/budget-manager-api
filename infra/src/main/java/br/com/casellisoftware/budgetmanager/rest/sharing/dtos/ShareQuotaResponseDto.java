package br.com.casellisoftware.budgetmanager.rest.sharing.dtos;

import java.math.BigDecimal;
import java.util.List;

public record ShareQuotaResponseDto(
        String payerId,
        String payerName,
        BigDecimal ratio,
        BigDecimal amount,
        List<String> paymentIds
) {
}
