package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Payment {

    private final String id;
    private final Money amount;
    private final Instant paymentDate;
    private final String details;
    private final String expenseId;
    private final String walletId;
    private final String bulletId;

    public Payment(String id, Money amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        this.id = id;
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
        this.paymentDate = paymentDate;
        this.details = details;
        this.expenseId = expenseId;
        this.walletId = walletId;
        this.bulletId = bulletId;
    }

    public static Payment create(Money amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        return new Payment(UUID.randomUUID().toString(), amount, paymentDate, details, expenseId, walletId, bulletId);
    }

    public static Payment rebuild(String id, Money amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        return new Payment(id, amount, paymentDate, details, expenseId, walletId, bulletId);
    }

    public Payment patch(PaymentPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        Money patchedAmount = patch.amount().orElse(this.amount);
        String patchedDetails = patch.details().orElse(this.details);

        if (Objects.equals(this.amount, patchedAmount)
                && Objects.equals(this.details, patchedDetails)) {
            return this;
        }

        return new Payment(this.id, patchedAmount, this.paymentDate, patchedDetails, this.expenseId, this.walletId, this.bulletId);
    }

    public String getId() {
        return id;
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
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
