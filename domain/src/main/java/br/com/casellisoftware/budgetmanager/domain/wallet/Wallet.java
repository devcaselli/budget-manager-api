package br.com.casellisoftware.budgetmanager.domain.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Wallet {

    private String id;
    private LocalDateTime date;
    private LocalDateTime closedDate;
    private Boolean closed;
    private BigDecimal amount;
    private BigDecimal remaining;

    public Wallet() {
    }

    public Wallet(String id, LocalDateTime date, LocalDateTime closedDate, Boolean closed, BigDecimal amount, BigDecimal remaining) {
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

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public LocalDateTime getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(LocalDateTime closedDate) {
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
