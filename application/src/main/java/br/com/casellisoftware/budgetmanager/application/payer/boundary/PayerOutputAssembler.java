package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import br.com.casellisoftware.budgetmanager.application.payer.usecase.PayerAmountDue;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

public final class PayerOutputAssembler {

    private PayerOutputAssembler() {
    }

    /**
     * Legacy assembler used by paths that don't compute a structured
     * {@link PayerAmountDue}. Carries {@code amountDue} into both
     * {@code monthlyAmount} and {@code journeyAmount} so the front always
     * sees a non-null value.
     */
    public static PayerOutput from(Payer payer, Money amountDue) {
        return new PayerOutput(
                payer.getId(),
                payer.getName(),
                payer.getType(),
                payer.getWalletId(),
                payer.getSubscriptionId(),
                payer.getPaymentDate(),
                amountDue,
                amountDue,
                amountDue,
                payer.isDeleted()
        );
    }

    public static PayerOutput from(Payer payer, PayerAmountDue amountDue) {
        return new PayerOutput(
                payer.getId(),
                payer.getName(),
                payer.getType(),
                payer.getWalletId(),
                payer.getSubscriptionId(),
                payer.getPaymentDate(),
                amountDue.monthly(),
                amountDue.monthly(),
                amountDue.journey(),
                payer.isDeleted()
        );
    }
}
