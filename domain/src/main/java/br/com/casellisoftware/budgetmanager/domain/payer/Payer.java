package br.com.casellisoftware.budgetmanager.domain.payer;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Payer {

    public static final int MAX_NAME_LENGTH = 120;
    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String name;
    private final PayerType type;
    private final String walletId;
    private final String subscriptionId;
    private final LocalDate paymentDate;
    private final boolean deleted;

    public Payer(String id,
                 String ownerId,
                 String name,
                 PayerType type,
                 String walletId,
                 String subscriptionId,
                 LocalDate paymentDate,
                 boolean deleted) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.name = validateName(name);
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.walletId = normalizeWalletId(walletId);
        validateLifecycle(this.type, this.walletId);
        this.subscriptionId = normalizeSubscriptionId(subscriptionId);
        this.paymentDate = Objects.requireNonNull(paymentDate, "paymentDate must not be null");
        this.deleted = deleted;
    }

    public Payer(String id,
                 String ownerId,
                 String name,
                 PayerType type,
                 String subscriptionId,
                 LocalDate paymentDate,
                 boolean deleted) {
        this(id, ownerId, name, type, null, subscriptionId, paymentDate, deleted);
    }

    public Payer(String id,
                 String name,
                 PayerType type,
                 String walletId,
                 String subscriptionId,
                 LocalDate paymentDate,
                 boolean deleted) {
        this(id, LEGACY_OWNER_ID, name, type, walletId, subscriptionId, paymentDate, deleted);
    }

    public Payer(String id,
                 String name,
                 PayerType type,
                 String subscriptionId,
                 LocalDate paymentDate,
                 boolean deleted) {
        this(id, LEGACY_OWNER_ID, name, type, null, subscriptionId, paymentDate, deleted);
    }

    public static Payer create(String name,
                               PayerType type,
                               String walletId,
                               String subscriptionId,
                               LocalDate paymentDate,
                               String ownerId) {
        return new Payer(
                UUID.randomUUID().toString(),
                ownerId == null ? LEGACY_OWNER_ID : ownerId,
                name,
                type,
                walletId,
                subscriptionId,
                paymentDate,
                false
        );
    }

    public static Payer create(String name,
                               PayerType type,
                               String subscriptionId,
                               LocalDate paymentDate,
                               String ownerId) {
        return create(name, type, null, subscriptionId, paymentDate, ownerId);
    }

    public static Payer create(String name,
                               PayerType type,
                               String walletId,
                               String subscriptionId,
                               LocalDate paymentDate) {
        return create(name, type, walletId, subscriptionId, paymentDate, LEGACY_OWNER_ID);
    }

    public static Payer create(String name,
                               PayerType type,
                               String subscriptionId,
                               LocalDate paymentDate) {
        return create(name, type, null, subscriptionId, paymentDate, LEGACY_OWNER_ID);
    }

    public Payer rename(String name) {
        String renamed = validateName(name);
        if (Objects.equals(this.name, renamed)) {
            return this;
        }
        return copy(renamed, this.type, this.walletId, this.subscriptionId, this.paymentDate, this.deleted);
    }

    public Payer changeType(PayerType type) {
        return changeLifecycle(type, this.walletId);
    }

    public Payer changeWalletId(String walletId) {
        return changeLifecycle(this.type, walletId);
    }

    public Payer changeLifecycle(PayerType type, String walletId) {
        PayerType updatedType = Objects.requireNonNull(type, "type must not be null");
        String updatedWalletId = normalizeWalletId(walletId);
        if (Objects.equals(this.type, updatedType) && Objects.equals(this.walletId, updatedWalletId)) {
            return this;
        }
        validateLifecycle(updatedType, updatedWalletId);
        return copy(this.name, updatedType, updatedWalletId, this.subscriptionId, this.paymentDate, this.deleted);
    }

    public Payer changeSubscription(String subscriptionId) {
        String updated = normalizeSubscriptionId(subscriptionId);
        if (Objects.equals(this.subscriptionId, updated)) {
            return this;
        }
        return copy(this.name, this.type, this.walletId, updated, this.paymentDate, this.deleted);
    }

    public Payer changePaymentDate(LocalDate paymentDate) {
        LocalDate updated = Objects.requireNonNull(paymentDate, "paymentDate must not be null");
        if (Objects.equals(this.paymentDate, updated)) {
            return this;
        }
        return copy(this.name, this.type, this.walletId, this.subscriptionId, updated, this.deleted);
    }

    public Payer delete() {
        if (deleted) {
            return this;
        }
        return copy(this.name, this.type, this.walletId, this.subscriptionId, this.paymentDate, true);
    }

    public Payer patch(PayerPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        String patchedName = patch.name().orElse(this.name);
        PayerType patchedType = patch.type().orElse(this.type);
        String patchedWalletId = patch.walletId().orElse(this.walletId);
        String patchedSubscriptionId = patch.subscriptionId().orElse(this.subscriptionId);
        LocalDate patchedPaymentDate = patch.paymentDate().orElse(this.paymentDate);

        if (Objects.equals(this.name, patchedName)
                && Objects.equals(this.type, patchedType)
                && Objects.equals(this.walletId, patchedWalletId)
                && Objects.equals(this.subscriptionId, patchedSubscriptionId)
                && Objects.equals(this.paymentDate, patchedPaymentDate)) {
            return this;
        }

        return new Payer(
                this.id,
                this.ownerId,
                patchedName,
                patchedType,
                patchedWalletId,
                patchedSubscriptionId,
                patchedPaymentDate,
                this.deleted
        );
    }

    private Payer copy(String name,
                       PayerType type,
                       String walletId,
                       String subscriptionId,
                       LocalDate paymentDate,
                       boolean deleted) {
        return new Payer(this.id, this.ownerId, name, type, walletId, subscriptionId, paymentDate, deleted);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public PayerType getType() {
        return type;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Payer payer
                && Objects.equals(id, payer.id)
                && Objects.equals(ownerId, payer.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    private static String normalizeSubscriptionId(String subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }
        if (subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscriptionId must not be blank");
        }
        return subscriptionId;
    }

    private static String normalizeWalletId(String walletId) {
        if (walletId == null) {
            return null;
        }
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
        return walletId;
    }

    private static void validateLifecycle(PayerType type, String walletId) {
        if (type == PayerType.STANDING && walletId != null) {
            throw new IllegalArgumentException("walletId must be null for STANDING payer");
        }
        if (type == PayerType.TRANSIENT && walletId == null) {
            throw new IllegalArgumentException("walletId must not be null for TRANSIENT payer");
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
