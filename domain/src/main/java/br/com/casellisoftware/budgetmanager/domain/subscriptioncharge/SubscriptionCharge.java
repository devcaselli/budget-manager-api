package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing the materialized monthly charge of a subscription.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New charges should be obtained via {@link #create}; reconstruction from
 * persistence uses {@link #rebuild}.</p>
 */
public final class SubscriptionCharge implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String subscriptionId;
    private final String walletId;
    private final YearMonth month;
    private final Money amount;
    private final Money remaining;
    private final FlagEnum flag;

    public SubscriptionCharge(String id,
                              String subscriptionId,
                              String walletId,
                              YearMonth month,
                              Money amount,
                              Money remaining,
                              FlagEnum flag) {
        this(id, LEGACY_OWNER_ID, subscriptionId, walletId, month, amount, remaining, flag);
    }

    public SubscriptionCharge(String id,
                              String ownerId,
                              String subscriptionId,
                              String walletId,
                              YearMonth month,
                              Money amount,
                              Money remaining,
                              FlagEnum flag) {
        this.id = id;
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.subscriptionId = subscriptionId;
        this.walletId = walletId;
        this.month = month;
        this.amount = amount;
        this.remaining = remaining;
        this.flag = flag == null ? FlagEnum.NONE : flag;
    }

    public static SubscriptionCharge create(String subscriptionId, String walletId, YearMonth month, Money amount, FlagEnum flag) {
        return create(subscriptionId, walletId, month, amount, flag, LEGACY_OWNER_ID);
    }

    public static SubscriptionCharge create(String subscriptionId, String walletId, YearMonth month, Money amount, FlagEnum flag, String ownerId) {
        validateId(subscriptionId, "subscriptionId");
        validateId(walletId, "walletId");
        Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return new SubscriptionCharge(UUID.randomUUID().toString(), ownerId, subscriptionId, walletId, month, amount, amount, flag);
    }

    public static SubscriptionCharge rebuild(String id,
                                             String subscriptionId,
                                             String walletId,
                                             YearMonth month,
                                             Money amount,
                                             Money remaining,
                                             FlagEnum flag) {
        return rebuild(id, subscriptionId, walletId, month, amount, remaining, flag, LEGACY_OWNER_ID);
    }

    public static SubscriptionCharge rebuild(String id,
                                             String subscriptionId,
                                             String walletId,
                                             YearMonth month,
                                             Money amount,
                                             Money remaining,
                                             FlagEnum flag,
                                             String ownerId) {
        return new SubscriptionCharge(id, ownerId, subscriptionId, walletId, month, amount, remaining, flag);
    }

    public SubscriptionCharge debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new SubscriptionCharge(this.id, this.ownerId, this.subscriptionId, this.walletId, this.month, this.amount, newRemaining, this.flag);
    }

    /**
     * Returns a new {@code SubscriptionCharge} with {@code remaining} increased by {@code amount}.
     * Used to symmetrically reverse a prior {@link #debit(Money)} (e.g. when a Share
     * is created or reverted on the current-month charge). Caps at the original {@code amount}
     * since you cannot credit more than was originally available.
     */
    public SubscriptionCharge credit(Money amount) {
        Money newRemaining = this.remaining.creditBy(amount);
        if (newRemaining.isGreaterThan(this.amount)) {
            throw new IllegalArgumentException(
                    "credit overflows charge amount: " + newRemaining.amount() + " > " + this.amount.amount());
        }
        return new SubscriptionCharge(this.id, this.ownerId, this.subscriptionId, this.walletId, this.month, this.amount, newRemaining, this.flag);
    }

    public SubscriptionCharge pay(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");
        return debit(payment.getAmount());
    }

    public Money consumed() {
        return this.amount.subtract(this.remaining);
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getWalletId() {
        return walletId;
    }

    public YearMonth getMonth() {
        return month;
    }

    public Money getAmount() {
        return amount;
    }

    public Money getRemaining() {
        return remaining;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubscriptionCharge charge
                && Objects.equals(id, charge.id)
                && Objects.equals(ownerId, charge.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static void validateId(String id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
