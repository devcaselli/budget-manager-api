package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final Money amount;
    private final Instant paymentDate;
    private final String details;
    private final String expenseId;
    private final String walletId;
    private final String bulletId;
    private final FlagEnum flag;
    private final PaymentKind kind;
    private final String payerId;
    private final String shareId;
    private final boolean reversal;
    private final String reversedPaymentId;

    public Payment(String id,
                   Money amount,
                   Instant paymentDate,
                   String details,
                   String expenseId,
                   String walletId,
                   String bulletId,
                   FlagEnum flag) {
        this(id, LEGACY_OWNER_ID, amount, paymentDate, details, expenseId, walletId, bulletId, flag);
    }

    public Payment(String id,
                   String ownerId,
                   Money amount,
                   Instant paymentDate,
                   String details,
                   String expenseId,
                   String walletId,
                   String bulletId,
                   FlagEnum flag) {
        this(
                id,
                ownerId,
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                PaymentKind.NORMAL,
                null,
                null,
                false,
                null
        );
    }

    public Payment(String id,
                   String ownerId,
                   Money amount,
                   Instant paymentDate,
                   String details,
                   String expenseId,
                   String walletId,
                   String bulletId,
                   FlagEnum flag,
                   PaymentKind kind,
                   String payerId,
                   String shareId,
                   boolean reversal,
                   String reversedPaymentId) {
        this.id = id;
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.paymentDate = paymentDate;
        this.details = details;
        this.expenseId = expenseId;
        this.walletId = walletId;
        this.bulletId = bulletId;
        this.flag = flag == null ? FlagEnum.NONE : flag;
        this.kind = kind == null ? PaymentKind.NORMAL : kind;
        this.payerId = normalizeNullableId(payerId, "payerId");
        this.shareId = normalizeNullableId(shareId, "shareId");
        this.reversal = reversal;
        this.reversedPaymentId = normalizeNullableId(reversedPaymentId, "reversedPaymentId");
        validateInvariants();
    }

    public Payment(String id,
                   Money amount,
                   Instant paymentDate,
                   String details,
                   String expenseId,
                   String walletId,
                   String bulletId) {
        this(id, amount, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE);
    }

    public static Payment create(Money amount,
                                 Instant paymentDate,
                                 String details,
                                 String expenseId,
                                 String walletId,
                                 String bulletId) {
        return create(amount, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE, LEGACY_OWNER_ID);
    }

    public static Payment create(Money amount,
                                 Instant paymentDate,
                                 String details,
                                 String expenseId,
                                 String walletId,
                                 String bulletId,
                                 FlagEnum flag) {
        return create(amount, paymentDate, details, expenseId, walletId, bulletId, flag, LEGACY_OWNER_ID);
    }

    public static Payment create(Money amount,
                                 Instant paymentDate,
                                 String details,
                                 String expenseId,
                                 String walletId,
                                 String bulletId,
                                 FlagEnum flag,
                                 String ownerId) {
        return new Payment(
                UUID.randomUUID().toString(),
                ownerId,
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                PaymentKind.NORMAL,
                null,
                null,
                false,
                null
        );
    }

    public static Payment createShared(Money amount,
                                       Instant paymentDate,
                                       String details,
                                       String expenseId,
                                       String walletId,
                                       String bulletId,
                                       String payerId,
                                       String shareId,
                                       FlagEnum flag) {
        return createShared(
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                payerId,
                shareId,
                flag,
                LEGACY_OWNER_ID
        );
    }

    public static Payment createShared(Money amount,
                                       Instant paymentDate,
                                       String details,
                                       String expenseId,
                                       String walletId,
                                       String bulletId,
                                       String payerId,
                                       String shareId,
                                       FlagEnum flag,
                                       String ownerId) {
        return new Payment(
                UUID.randomUUID().toString(),
                ownerId,
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                PaymentKind.SHARED,
                payerId,
                shareId,
                false,
                null
        );
    }

    public static Payment createReversal(Payment original,
                                         Instant paymentDate,
                                         String details,
                                         String ownerId) {
        Objects.requireNonNull(original, "original must not be null");
        return createReversal(
                original.getAmount(),
                paymentDate,
                details,
                original.getExpenseId(),
                original.getWalletId(),
                original.getBulletId(),
                original.getKind(),
                original.getPayerId(),
                original.getShareId(),
                original.getId(),
                original.getFlag(),
                ownerId
        );
    }

    public static Payment createReversal(Money amount,
                                         Instant paymentDate,
                                         String details,
                                         String expenseId,
                                         String walletId,
                                         String bulletId,
                                         PaymentKind kind,
                                         String payerId,
                                         String shareId,
                                         String reversedPaymentId,
                                         FlagEnum flag,
                                         String ownerId) {
        return new Payment(
                UUID.randomUUID().toString(),
                ownerId,
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                kind,
                payerId,
                shareId,
                true,
                reversedPaymentId
        );
    }

    public static Payment rebuild(String id,
                                  Money amount,
                                  Instant paymentDate,
                                  String details,
                                  String expenseId,
                                  String walletId,
                                  String bulletId) {
        return rebuild(id, amount, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE, LEGACY_OWNER_ID);
    }

    public static Payment rebuild(String id,
                                  Money amount,
                                  Instant paymentDate,
                                  String details,
                                  String expenseId,
                                  String walletId,
                                  String bulletId,
                                  FlagEnum flag) {
        return rebuild(id, amount, paymentDate, details, expenseId, walletId, bulletId, flag, LEGACY_OWNER_ID);
    }

    public static Payment rebuild(String id,
                                  Money amount,
                                  Instant paymentDate,
                                  String details,
                                  String expenseId,
                                  String walletId,
                                  String bulletId,
                                  FlagEnum flag,
                                  String ownerId) {
        return new Payment(id, ownerId, amount, paymentDate, details, expenseId, walletId, bulletId, flag);
    }

    public static Payment rebuild(String id,
                                  Money amount,
                                  Instant paymentDate,
                                  String details,
                                  String expenseId,
                                  String walletId,
                                  String bulletId,
                                  FlagEnum flag,
                                  String ownerId,
                                  PaymentKind kind,
                                  String payerId,
                                  String shareId,
                                  boolean reversal,
                                  String reversedPaymentId) {
        return new Payment(
                id,
                ownerId,
                amount,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                kind,
                payerId,
                shareId,
                reversal,
                reversedPaymentId
        );
    }

    public Payment patch(PaymentPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        Money patchedAmount = patch.amount().orElse(this.amount);
        String patchedDetails = patch.details().orElse(this.details);
        FlagEnum patchedFlag = patch.flag().orElse(this.flag);

        if (Objects.equals(this.amount, patchedAmount)
                && Objects.equals(this.details, patchedDetails)
                && Objects.equals(this.flag, patchedFlag)) {
            return this;
        }

        return new Payment(
                this.id,
                this.ownerId,
                patchedAmount,
                this.paymentDate,
                patchedDetails,
                this.expenseId,
                this.walletId,
                this.bulletId,
                patchedFlag,
                this.kind,
                this.payerId,
                this.shareId,
                this.reversal,
                this.reversedPaymentId
        );
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public String getDetails() {
        return details;
    }

    public String getExpenseId() {
        return expenseId;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getBulletId() {
        return bulletId;
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    public PaymentKind getKind() {
        return kind;
    }

    public String getPayerId() {
        return payerId;
    }

    public String getShareId() {
        return shareId;
    }

    public boolean isReversal() {
        return reversal;
    }

    public String getReversedPaymentId() {
        return reversedPaymentId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Payment payment
                && Objects.equals(id, payment.id)
                && Objects.equals(ownerId, payment.ownerId);
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

    private static String normalizeNullableId(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private void validateInvariants() {
        if (kind == PaymentKind.SHARED && shareId == null) {
            throw new IllegalArgumentException("shareId must not be null when kind is SHARED");
        }
        if (reversal && reversedPaymentId == null) {
            throw new IllegalArgumentException("reversedPaymentId must not be null when reversal=true");
        }
        if (!reversal && reversedPaymentId != null) {
            throw new IllegalArgumentException("reversedPaymentId must be null when reversal=false");
        }
    }
}
