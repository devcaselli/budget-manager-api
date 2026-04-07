package br.com.casellisoftware.budgetmanager.domain.wallet;

import java.math.BigDecimal;
import java.time.Instant;

public class Wallet {

    private String id;
    private Instant date;
    private Instant closedDate;
    private Boolean closed;
    private BigDecimal amount;
    private BigDecimal remaining;

    public Wallet() {
    }

    public Wallet(String id, Instant date, Instant closedDate, Boolean closed, BigDecimal amount, BigDecimal remaining) {
        this.id = id;
        this.date = date;
        this.closedDate = closedDate;
        this.closed = closed;
        this.amount = amount;
        this.remaining = remaining;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public Instant getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Instant closedDate) {
        this.closedDate = closedDate;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }
}
