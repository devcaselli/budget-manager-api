package br.com.casellisoftware.budgetmanager.domain.sharing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record ShareQuota(
        String payerId,
        BigDecimal ratio,
        List<String> paymentIds
) {
    public ShareQuota {
        Objects.requireNonNull(payerId, "payerId must not be null");
        if (payerId.isBlank()) {
            throw new IllegalArgumentException("payerId must not be blank");
        }
        Objects.requireNonNull(ratio, "ratio must not be null");
        if (ratio.signum() <= 0) {
            throw new IllegalArgumentException("ratio must be positive");
        }
        Objects.requireNonNull(paymentIds, "paymentIds must not be null");
        paymentIds = List.copyOf(paymentIds);
    }
}
