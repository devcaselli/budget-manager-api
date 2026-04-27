package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletPatch;

public final class PatchWalletInputAssembler {

    private PatchWalletInputAssembler() {
    }

    public static WalletPatch toPatch(PatchWalletInput input) {
        return WalletPatch.empty()
                .withDescription(input.description())
                .withBudget(input.budget() == null ? null : Money.of(input.budget()))
                .withClosedDate(input.closedDate())
                .withClosed(input.closed());
    }
}
