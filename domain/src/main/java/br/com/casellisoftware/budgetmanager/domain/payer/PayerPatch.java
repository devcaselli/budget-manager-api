package br.com.casellisoftware.budgetmanager.domain.payer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PayerPatch(
        Optional<String> name,
        Optional<PayerType> type,
        Optional<String> walletId,
        Optional<String> subscriptionId,
        Optional<LocalDate> paymentDate
) {
    public PayerPatch {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        Objects.requireNonNull(paymentDate, "paymentDate must not be null");
    }

    public static PayerPatch empty() {
        return new PayerPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public PayerPatch withName(String name) {
        return name == null ? this : new PayerPatch(Optional.of(name), type, walletId, subscriptionId, paymentDate);
    }

    public PayerPatch withType(PayerType type) {
        return type == null ? this : new PayerPatch(name, Optional.of(type), walletId, subscriptionId, paymentDate);
    }

    public PayerPatch withWalletId(String walletId) {
        return walletId == null ? this : new PayerPatch(name, type, Optional.of(walletId), subscriptionId, paymentDate);
    }

    public PayerPatch withSubscriptionId(String subscriptionId) {
        return subscriptionId == null ? this : new PayerPatch(name, type, walletId, Optional.of(subscriptionId), paymentDate);
    }

    public PayerPatch withPaymentDate(LocalDate paymentDate) {
        return paymentDate == null ? this : new PayerPatch(name, type, walletId, subscriptionId, Optional.of(paymentDate));
    }

    public boolean isEmpty() {
        return name.isEmpty()
                && type.isEmpty()
                && walletId.isEmpty()
                && subscriptionId.isEmpty()
                && paymentDate.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        name.ifPresent(ignored -> fields.add("name"));
        type.ifPresent(ignored -> fields.add("type"));
        walletId.ifPresent(ignored -> fields.add("walletId"));
        subscriptionId.ifPresent(ignored -> fields.add("subscriptionId"));
        paymentDate.ifPresent(ignored -> fields.add("paymentDate"));
        return List.copyOf(fields);
    }
}
