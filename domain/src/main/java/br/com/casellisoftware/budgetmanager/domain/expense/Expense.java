package br.com.casellisoftware.budgetmanager.domain.expense;

import java.math.BigDecimal;
import java.time.Instant;

public class Expense {

    private String id;
    private String name;
    private BigDecimal cost;
    private Instant purchaseDate;
    private BigDecimal remaining;
    private String walletId;

    public Expense() {
    }

    public Expense(String id, String name, BigDecimal cost, Instant purchaseDate, BigDecimal remaining, String walletId) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.purchaseDate = purchaseDate;
        this.remaining = remaining;
        this.walletId = walletId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Instant getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(Instant purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }
}