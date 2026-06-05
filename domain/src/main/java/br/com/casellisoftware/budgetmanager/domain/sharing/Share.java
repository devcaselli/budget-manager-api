package br.com.casellisoftware.budgetmanager.domain.sharing;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Share {

    public static final int RATIO_SCALE = 8;
    private static final BigDecimal RATIO_TOLERANCE = new BigDecimal("0.000001");
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final String id;
    private final String ownerId;
    private final String walletId;
    private final ShareSourceType sourceType;
    private final String sourceId;
    private final Money totalAmount;
    private final Money ownerShare;
    private final BigDecimal ownerRatio;
    private final List<ShareQuota> quotas;
    private final ShareStatus status;
    private final List<String> paymentIds;
    private final Instant createdAt;
    private final Instant revertedAt;
    private final YearMonth stoppedFromMonth;

    public Share(String id,
                 String ownerId,
                 String walletId,
                 ShareSourceType sourceType,
                 String sourceId,
                 Money totalAmount,
                 Money ownerShare,
                 BigDecimal ownerRatio,
                 List<ShareQuota> quotas,
                 ShareStatus status,
                 List<String> paymentIds,
                 Instant createdAt,
                 Instant revertedAt,
                 YearMonth stoppedFromMonth) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.walletId = requireNonBlank(walletId, "walletId");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.sourceId = requireNonBlank(sourceId, "sourceId");
        this.totalAmount = Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        this.ownerShare = Objects.requireNonNull(ownerShare, "ownerShare must not be null");
        this.ownerRatio = normalizeRatio(ownerRatio);
        this.quotas = normalizeQuotas(quotas);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.paymentIds = normalizePaymentIds(paymentIds);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.revertedAt = revertedAt;
        this.stoppedFromMonth = stoppedFromMonth;

        validateCurrency();
        validateAmounts();
        validateRatioSum();
        if (this.status == ShareStatus.REVERTED && this.revertedAt == null) {
            throw new IllegalArgumentException("revertedAt must not be null when status is REVERTED");
        }
        if (this.stoppedFromMonth != null && this.sourceType == ShareSourceType.EXPENSE) {
            throw new IllegalArgumentException("stoppedFromMonth is only applicable to recurring sources");
        }
    }

    public static Share create(String walletId,
                               ShareSourceType sourceType,
                               String sourceId,
                               Money totalAmount,
                               Money ownerShare,
                               List<ShareQuotaAllocation> quotaAllocations,
                               String ownerId,
                               Instant createdAt) {
        Objects.requireNonNull(quotaAllocations, "quotaAllocations must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        validateAllocationCurrencies(totalAmount, ownerShare, quotaAllocations);

        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException("totalAmount must be positive");
        }
        if (totalAmount.amount().compareTo(AMOUNT_TOLERANCE) < 0) {
            throw new IllegalArgumentException("totalAmount must be at least " + AMOUNT_TOLERANCE);
        }
        if (ownerShare.isGreaterThan(totalAmount)) {
            throw new ShareRatioMismatchException("ownerShare must be less than or equal to totalAmount");
        }

        BigDecimal quotaAmountSum = quotaAllocations.stream()
                .map(allocation -> allocation.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = totalAmount.amount();
        BigDecimal resolvedTotal = ownerShare.amount().add(quotaAmountSum);
        if (resolvedTotal.subtract(total).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new ShareRatioMismatchException("ownerShare + quotas must equal totalAmount");
        }

        BigDecimal ownerRatio = ownerShare.amount().divide(total, RATIO_SCALE, RoundingMode.HALF_EVEN);
        List<ShareQuota> quotas = quotaAllocations.stream()
                .map(allocation -> new ShareQuota(
                        allocation.payerId(),
                        allocation.amount().amount().divide(total, RATIO_SCALE, RoundingMode.HALF_EVEN),
                        List.of()))
                .toList();

        return new Share(
                UUID.randomUUID().toString(),
                ownerId,
                walletId,
                sourceType,
                sourceId,
                totalAmount,
                ownerShare,
                ownerRatio,
                quotas,
                ShareStatus.ACTIVE,
                List.of(),
                createdAt,
                null,
                null
        );
    }

    public Share appendPayments(String ownerPaymentId, Map<String, String> paymentIdByPayerId) {
        if (status != ShareStatus.ACTIVE) {
            throw new IllegalStateException("cannot append payments for a reverted share");
        }
        Map<String, String> resolvedPaymentMap = paymentIdByPayerId == null ? Map.of() : paymentIdByPayerId;

        List<String> updatedPaymentIds = new ArrayList<>(this.paymentIds);
        if (ownerPaymentId != null && !ownerPaymentId.isBlank() && !updatedPaymentIds.contains(ownerPaymentId)) {
            updatedPaymentIds.add(ownerPaymentId);
        }

        List<ShareQuota> updatedQuotas = new ArrayList<>(this.quotas.size());
        for (ShareQuota quota : this.quotas) {
            String paymentId = resolvedPaymentMap.get(quota.payerId());
            if (paymentId == null || paymentId.isBlank()) {
                updatedQuotas.add(quota);
                continue;
            }

            List<String> quotaPaymentIds = new ArrayList<>(quota.paymentIds());
            if (!quotaPaymentIds.contains(paymentId)) {
                quotaPaymentIds.add(paymentId);
            }
            if (!updatedPaymentIds.contains(paymentId)) {
                updatedPaymentIds.add(paymentId);
            }
            updatedQuotas.add(new ShareQuota(quota.payerId(), quota.ratio(), quotaPaymentIds));
        }

        return new Share(
                this.id,
                this.ownerId,
                this.walletId,
                this.sourceType,
                this.sourceId,
                this.totalAmount,
                this.ownerShare,
                this.ownerRatio,
                updatedQuotas,
                this.status,
                updatedPaymentIds,
                this.createdAt,
                this.revertedAt,
                this.stoppedFromMonth
        );
    }

    public Share revert(Instant revertedAt) {
        Objects.requireNonNull(revertedAt, "revertedAt must not be null");
        if (this.status == ShareStatus.REVERTED) {
            throw new ShareAlreadyRevertedException(this.id);
        }
        return new Share(
                this.id,
                this.ownerId,
                this.walletId,
                this.sourceType,
                this.sourceId,
                this.totalAmount,
                this.ownerShare,
                this.ownerRatio,
                this.quotas,
                ShareStatus.REVERTED,
                this.paymentIds,
                this.createdAt,
                revertedAt,
                this.stoppedFromMonth
        );
    }

    /**
     * Stops the share from {@code fromMonth} onward without touching the past.
     *
     * <p>Non-destructive (unlike {@link #revert(Instant)}): the share stays
     * {@link ShareStatus#ACTIVE} and prior months keep settling at the shared
     * ratio. Read paths gate on {@link #isEffectiveFor(YearMonth)} so wallets at
     * month {@code >= stoppedFromMonth} stop applying the owner ratio.</p>
     *
     * <p>Earliest-wins: re-stopping at an earlier month shrinks the active
     * window; re-stopping at a later (or equal) month is a no-op.</p>
     */
    public Share stopFrom(YearMonth fromMonth) {
        Objects.requireNonNull(fromMonth, "fromMonth must not be null");
        if (this.status == ShareStatus.REVERTED) {
            throw new ShareStopNotApplicableException("cannot stop a reverted share: " + this.id);
        }
        if (this.sourceType == ShareSourceType.EXPENSE) {
            throw new ShareStopNotApplicableException("cannot stop an expense-sourced share: " + this.id);
        }
        YearMonth resolved = (this.stoppedFromMonth == null || fromMonth.isBefore(this.stoppedFromMonth))
                ? fromMonth
                : this.stoppedFromMonth;
        if (resolved.equals(this.stoppedFromMonth)) {
            return this;
        }
        return new Share(
                this.id,
                this.ownerId,
                this.walletId,
                this.sourceType,
                this.sourceId,
                this.totalAmount,
                this.ownerShare,
                this.ownerRatio,
                this.quotas,
                this.status,
                this.paymentIds,
                this.createdAt,
                this.revertedAt,
                resolved
        );
    }

    /**
     * Whether this share applies its owner ratio for a wallet at {@code walletMonth}.
     * Single source of truth for the month-forward stop rule — every read path
     * that consumes the active share MUST gate on this.
     */
    public boolean isEffectiveFor(YearMonth walletMonth) {
        Objects.requireNonNull(walletMonth, "walletMonth must not be null");
        return status == ShareStatus.ACTIVE
                && (stoppedFromMonth == null || walletMonth.isBefore(stoppedFromMonth));
    }

    public boolean isFullAssignment() {
        return this.ownerRatio.signum() == 0;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getWalletId() {
        return walletId;
    }

    public ShareSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Money getTotalAmount() {
        return totalAmount;
    }

    public Money getOwnerShare() {
        return ownerShare;
    }

    public BigDecimal getOwnerRatio() {
        return ownerRatio;
    }

    public List<ShareQuota> getQuotas() {
        return quotas;
    }

    public ShareStatus getStatus() {
        return status;
    }

    public List<String> getPaymentIds() {
        return paymentIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevertedAt() {
        return revertedAt;
    }

    public YearMonth getStoppedFromMonth() {
        return stoppedFromMonth;
    }

    private static BigDecimal normalizeRatio(BigDecimal ratio) {
        Objects.requireNonNull(ratio, "ownerRatio must not be null");
        return ratio.setScale(RATIO_SCALE, RoundingMode.HALF_EVEN);
    }

    private static List<ShareQuota> normalizeQuotas(List<ShareQuota> quotas) {
        Objects.requireNonNull(quotas, "quotas must not be null");
        if (quotas.isEmpty()) {
            throw new IllegalArgumentException("quotas must not be empty");
        }
        Set<String> payerIds = new HashSet<>();
        for (ShareQuota quota : quotas) {
            if (!payerIds.add(quota.payerId())) {
                throw new IllegalArgumentException("duplicate payerId in quotas: " + quota.payerId());
            }
        }
        return List.copyOf(quotas);
    }

    /**
     * Preserves caller insertion order and removes duplicates.
     *
     * <p>The aggregate exposes {@link #getPaymentIds()} as an ordered list — log
     * output, audit traces, and revert iteration all rely on a stable order.
     * Callers MUST pass an ordered collection (a {@code List}). A
     * {@code LinkedHashSet} is used here as a defensive dedupe that still
     * preserves insertion order, so accidental duplicates never silently
     * mutate the share's payment trail.</p>
     */
    private static List<String> normalizePaymentIds(List<String> paymentIds) {
        Objects.requireNonNull(paymentIds, "paymentIds must not be null");
        return List.copyOf(new LinkedHashSet<>(paymentIds));
    }

    private void validateCurrency() {
        if (!totalAmount.currency().equals(ownerShare.currency())) {
            throw new ShareCurrencyMismatchException(
                    "ownerShare currency must match totalAmount currency");
        }
    }

    private void validateAmounts() {
        if (!totalAmount.isPositive()) {
            throw new IllegalArgumentException("totalAmount must be positive");
        }
        if (ownerShare.amount().signum() < 0) {
            throw new IllegalArgumentException("ownerShare must not be negative");
        }
        if (ownerShare.isGreaterThan(totalAmount)) {
            throw new ShareRatioMismatchException("ownerShare must be less than or equal to totalAmount");
        }
    }

    private void validateRatioSum() {
        BigDecimal quotaRatioSum = quotas.stream()
                .map(ShareQuota::ratio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRatio = ownerRatio.add(quotaRatioSum);
        if (totalRatio.subtract(BigDecimal.ONE).abs().compareTo(RATIO_TOLERANCE) > 0) {
            throw new ShareRatioMismatchException("ownerRatio + quotas.ratio must equal 1");
        }
    }

    private static void validateAllocationCurrencies(Money totalAmount,
                                                     Money ownerShare,
                                                     List<ShareQuotaAllocation> quotaAllocations) {
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(ownerShare, "ownerShare must not be null");
        if (!totalAmount.currency().equals(ownerShare.currency())) {
            throw new ShareCurrencyMismatchException(
                    "ownerShare currency must match totalAmount currency");
        }
        for (ShareQuotaAllocation quotaAllocation : quotaAllocations) {
            if (!totalAmount.currency().equals(quotaAllocation.amount().currency())) {
                throw new ShareCurrencyMismatchException(
                        "quota currency must match totalAmount currency");
            }
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public record ShareQuotaAllocation(
            String payerId,
            Money amount
    ) {
        public ShareQuotaAllocation {
            Objects.requireNonNull(payerId, "payerId must not be null");
            if (payerId.isBlank()) {
                throw new IllegalArgumentException("payerId must not be blank");
            }
            Objects.requireNonNull(amount, "amount must not be null");
            if (!amount.isPositive()) {
                throw new IllegalArgumentException("amount must be positive");
            }
        }
    }
}
