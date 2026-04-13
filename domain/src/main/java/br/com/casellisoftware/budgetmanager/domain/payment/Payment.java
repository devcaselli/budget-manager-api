package br.com.casellisoftware.budgetmanager.domain.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private final String id;
    private final BigDecimal amount;
    private final Instant paymentDate;
    private final String details;
    private final String expenseId;
    private final String walletId;
    private final String bulletId;

    public  Payment(String id, BigDecimal amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.details = details;
        this.expenseId = expenseId;
        this.walletId = walletId;
        this.bulletId = bulletId;
    }

    public static Payment create(BigDecimal amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        return new Payment(UUID.randomUUID().toString(), amount, paymentDate, details, expenseId, walletId, bulletId);
    }

    public String getId() {
        return id;
    }

    public BigDecimal getAmount() {
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
