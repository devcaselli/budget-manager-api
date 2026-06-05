package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

public record ShareOutput(
        String id,
        String walletId,
        ShareSourceType sourceType,
        String sourceId,
        BigDecimal totalAmount,
        BigDecimal ownerShare,
        BigDecimal ownerRatio,
        String currency,
        ShareStatus status,
        List<ShareQuotaOutput> quotas,
        List<String> paymentIds,
        Instant createdAt,
        Instant revertedAt,
        YearMonth stoppedFromMonth
) {
}
