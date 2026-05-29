package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an expense.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New expenses should be obtained via {@link #create}; reconstruction
 * from persistence uses the public constructor directly (e.g. via MapStruct).</p>
 *
 * <p>{@code remaining} represents the portion of {@code cost} that has not yet been
 * settled. A newly created expense starts with {@code remaining == cost}; as the
 * expense is paid, {@code remaining} shrinks toward zero via {@link #debit(Money)}.</p>
 */
public final class Expense implements FlagAware {

    public static final int MAX_NAME_LENGTH = 120;
    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String walletId;
    private final String creditCardId;
    private final String name;
    private final Money cost;
    private final Money remaining;
    private final LocalDate purchaseDate;
    private final List<String> paymentIds;
    private final FlagEnum flag;
    private final boolean hidden;
    private final String installmentId;
    /** Optional external id from ingest-api used for deduplication on sync. */
    private final String sourcePendingId;

    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag) {
        this(id, walletId, creditCardId, name, cost, remaining, purchaseDate, paymentIds, flag, false, null);
    }

    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag,
                   String ownerId) {
        this(id, walletId, creditCardId, name, cost, remaining, purchaseDate, paymentIds, flag, false, null, ownerId);
    }

    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag,
                   boolean hidden) {
        this(id, walletId, creditCardId, name, cost, remaining, purchaseDate, paymentIds, flag, hidden, null);
    }

    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag,
                   boolean hidden,
                   String installmentId) {
        this(id, walletId, creditCardId, name, cost, remaining, purchaseDate, paymentIds, flag, hidden, installmentId, LEGACY_OWNER_ID);
    }

    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag,
                   boolean hidden,
                   String installmentId,
                   String ownerId) {
        this(id, walletId, creditCardId, name, cost, remaining, purchaseDate, paymentIds, flag, hidden, installmentId, ownerId, null);
    }

    /** Canonical constructor — all fields. */
    public Expense(String id,
                   String walletId,
                   String creditCardId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds,
                   FlagEnum flag,
                   boolean hidden,
                   String installmentId,
                   String ownerId,
                   String sourcePendingId) {
        this.id = id;
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.walletId = walletId;
        this.creditCardId = Objects.requireNonNull(creditCardId, "creditCardId must not be null");
        this.name = name;
        this.cost = cost;
        this.remaining = remaining;
        this.purchaseDate = purchaseDate;
        this.paymentIds = paymentIds != null ? List.copyOf(paymentIds) : List.of();
        this.flag = flag == null ? FlagEnum.NONE : flag;
        this.hidden = hidden;
        this.installmentId = normalizeInstallmentId(installmentId);
        this.sourcePendingId = sourcePendingId;
    }

    /**
     * Creates a brand-new {@code Expense}. Generates a fresh id, initializes
     * {@code remaining} equal to {@code cost}, and enforces business rules:
     * cost must be positive and purchaseDate must not be in the future.
     */
    public static Expense create(String walletId,
                                 String creditCardId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate,
                                 FlagEnum flag) {
        return create(walletId, creditCardId, name, cost, purchaseDate, flag, false);
    }

    public static Expense create(String walletId,
                                 String creditCardId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate,
                                 FlagEnum flag,
                                 boolean hidden) {
        return create(walletId, creditCardId, name, cost, purchaseDate, flag, hidden, null);
    }

    public static Expense create(String walletId,
                                 String creditCardId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate,
                                 FlagEnum flag,
                                 boolean hidden,
                                 String installmentId) {
        return create(walletId, creditCardId, name, cost, purchaseDate, flag, hidden, installmentId, LEGACY_OWNER_ID);
    }

    public static Expense create(String walletId,
                                 String creditCardId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate,
                                 FlagEnum flag,
                                 boolean hidden,
                                 String installmentId,
                                 String ownerId) {
        return create(walletId, creditCardId, name, cost, purchaseDate, flag, hidden, installmentId, ownerId, null);
    }

    /**
     * Creates a brand-new {@code Expense} originated from an ingest-sync pending item.
     * {@code sourcePendingId} is stored for deduplication: a second sync skips any pending
     * item whose id already matches a saved expense.
     */
    public static Expense create(String walletId,
                                 String creditCardId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate,
                                 FlagEnum flag,
                                 boolean hidden,
                                 String installmentId,
                                 String ownerId,
                                 String sourcePendingId) {
        validateName(name);
        if (!cost.isPositive()) {
            throw new IllegalArgumentException("cost must be positive");
        }
        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("purchaseDate must not be in the future");
        }
        return new Expense(
                UUID.randomUUID().toString(),
                walletId,
                creditCardId,
                name,
                cost,
                cost,
                purchaseDate,
                List.of(),
                flag == null ? FlagEnum.NONE : flag,
                hidden,
                installmentId,
                ownerId,
                sourcePendingId
        );
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
    }

    private static String normalizeInstallmentId(String installmentId) {
        if (installmentId == null) {
            return null;
        }
        if (installmentId.isBlank()) {
            throw new IllegalArgumentException("installmentId must not be blank");
        }
        return installmentId;
    }

    /**
     * Returns a new {@code Expense} with {@code remaining} reduced by {@code amount}.
     */
    public Expense debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Expense(this.id, this.walletId, this.creditCardId, this.name, this.cost, newRemaining,
                this.purchaseDate, this.paymentIds, this.flag, this.hidden, this.installmentId, this.ownerId);
    }

    /**
     * Returns a new {@code Expense} with {@code remaining} increased by {@code amount}.
     * Caps at {@code cost} — refusing to credit above the original cost.
     * Symmetric inverse of {@link #debit(Money)} used when reversing prior shared payments.
     */
    public Expense credit(Money amount) {
        Money newRemaining = this.remaining.creditBy(amount);
        if (newRemaining.isGreaterThan(this.cost)) {
            throw new IllegalArgumentException(
                    "credit overflows expense cost: " + newRemaining.amount() + " > " + this.cost.amount());
        }
        return new Expense(this.id, this.walletId, this.creditCardId, this.name, this.cost, newRemaining,
                this.purchaseDate, this.paymentIds, this.flag, this.hidden, this.installmentId, this.ownerId);
    }

    /**
     * Returns a new {@code Expense} with {@code payment.getId()} appended to
     * {@code paymentIds} and {@code remaining} debited by {@code payment.getAmount()}.
     */
    public Expense pay(Payment payment) {

        List<String> updatedIds = new ArrayList<>(this.paymentIds.size() + 1);
        updatedIds.addAll(this.paymentIds);
        updatedIds.add(payment.getId());

        Money newRemaining = this.remaining.debitBy(payment.getAmount());
        return new Expense(this.id, this.walletId, this.creditCardId, this.name, this.cost, newRemaining,
                this.purchaseDate, updatedIds, this.flag, this.hidden, this.installmentId, this.ownerId);
    }

    /**
     * Applies an explicit partial update. Financial state derived from payments
     * ({@code remaining} and {@code paymentIds}) is intentionally not patchable.
     */
    public Expense patch(ExpensePatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        patch.name().ifPresent(Expense::validateName);

        String patchedName = patch.name().orElse(this.name);
        Money patchedCost = patch.cost().orElse(this.cost);
        LocalDate patchedPurchaseDate = patch.purchaseDate().orElse(this.purchaseDate);
        FlagEnum patchedFlag = patch.flag().orElse(this.flag);

        if (Objects.equals(this.name, patchedName)
                && Objects.equals(this.cost, patchedCost)
                && Objects.equals(this.purchaseDate, patchedPurchaseDate)
                && Objects.equals(this.flag, patchedFlag)) {
            return this;
        }

        return copyWith(patchedName, patchedCost, patchedPurchaseDate, patchedFlag);
    }

    private Expense copyWith(String name, Money cost, LocalDate purchaseDate, FlagEnum flag) {
        return new Expense(this.id, this.walletId, this.creditCardId, name, cost, this.remaining,
                purchaseDate, this.paymentIds, flag, this.hidden, this.installmentId, this.ownerId);
    }

    /**
     * Returns a new {@code Expense} marked as hidden (off-balance). Idempotent:
     * if already hidden, returns {@code this}.
     */
    public Expense hide() {
        if (this.hidden) {
            return this;
        }
        return copyWithHidden(true);
    }

    /**
     * Returns a new {@code Expense} marked as visible. Idempotent:
     * if already visible, returns {@code this}.
     */
    public Expense unhide() {
        if (!this.hidden) {
            return this;
        }
        return copyWithHidden(false);
    }

    private Expense copyWithHidden(boolean hidden) {
        return new Expense(this.id, this.walletId, this.creditCardId, this.name, this.cost, this.remaining,
                this.purchaseDate, this.paymentIds, this.flag, hidden, this.installmentId, this.ownerId);
    }

    /**
     * Returns a new {@code Expense} with {@code cost} and {@code remaining} reset
     * to {@code amount}. Used to restore an installment-derived monthly expense
     * to its full value after the associated share is reverted.
     */
    public Expense restoreAmount(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Expense(this.id, this.walletId, this.creditCardId, this.name, amount, amount,
                this.purchaseDate, this.paymentIds, this.flag, false, this.installmentId, this.ownerId);
    }

    public Expense linkToInstallment(String installmentId) {
        return new Expense(
                this.id,
                this.walletId,
                this.creditCardId,
                this.name,
                this.cost,
                this.remaining,
                this.purchaseDate,
                this.paymentIds,
                this.flag,
                this.hidden,
                Objects.requireNonNull(installmentId, "installmentId must not be null"),
                this.ownerId
        );
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

    public String getCreditCardId() {
        return creditCardId;
    }

    public String getName() {
        return name;
    }

    public Money getCost() {
        return cost;
    }

    public Money getRemaining() {
        return remaining;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public List<String> getPaymentIds() {
        return paymentIds;
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    public boolean isHidden() {
        return hidden;
    }

    public String getInstallmentId() {
        return installmentId;
    }

    public String getSourcePendingId() {
        return sourcePendingId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Expense expense
                && Objects.equals(id, expense.id)
                && Objects.equals(ownerId, expense.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
