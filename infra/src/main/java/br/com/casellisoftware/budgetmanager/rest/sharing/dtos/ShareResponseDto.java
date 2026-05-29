package br.com.casellisoftware.budgetmanager.rest.sharing.dtos;

import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ShareResponseDto(
        String id,
        String walletId,
        ShareSourceType sourceType,
        String sourceId,
        BigDecimal totalAmount,
        BigDecimal ownerShare,
        BigDecimal ownerRatio,
        String currency,
        ShareStatus status,
        List<ShareQuotaResponseDto> quotas,
        List<String> paymentIds,
        Instant createdAt,
        Instant revertedAt
) {
}
