package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.payment.PaymentPatch;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

public final class PatchPaymentInputAssembler {

    private PatchPaymentInputAssembler() {
    }

    public static PaymentPatch toPatch(PatchPaymentInput input) {
        return PaymentPatch.empty()
                .withAmount(input.amount() == null ? null : Money.of(input.amount()))
                .withDetails(input.details());
    }
}
