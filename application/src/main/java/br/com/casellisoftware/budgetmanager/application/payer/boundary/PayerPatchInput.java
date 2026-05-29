package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import br.com.casellisoftware.budgetmanager.domain.payer.PayerPatch;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PayerPatchInput(
        Optional<String> name,
        Optional<PayerType> type,
        Optional<String> walletId,
        Optional<String> subscriptionId,
        Optional<LocalDate> paymentDate
) {
    public PayerPatchInput {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        Objects.requireNonNull(paymentDate, "paymentDate must not be null");
    }

    public static PayerPatchInput empty() {
        return new PayerPatchInput(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public PayerPatch toPatch() {
        return new PayerPatch(name, type, walletId, subscriptionId, paymentDate);
    }
}
