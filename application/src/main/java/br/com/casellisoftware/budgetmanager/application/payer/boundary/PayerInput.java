package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;

import java.time.LocalDate;

public record PayerInput(
        String name,
        PayerType type,
        String walletId,
        String subscriptionId,
        LocalDate paymentDate,
        String ownerId
) {
    public PayerInput(String name, PayerType type, String walletId, String subscriptionId, LocalDate paymentDate) {
        this(name, type, walletId, subscriptionId, paymentDate, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PayerInput(String name, PayerType type, String subscriptionId, LocalDate paymentDate) {
        this(name, type, null, subscriptionId, paymentDate, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PayerInput withOwnerId(String ownerId) {
        return new PayerInput(name, type, walletId, subscriptionId, paymentDate, ownerId);
    }
}
