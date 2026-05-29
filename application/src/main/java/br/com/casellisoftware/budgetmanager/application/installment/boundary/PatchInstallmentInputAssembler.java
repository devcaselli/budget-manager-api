package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentPatch;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Currency;

public final class PatchInstallmentInputAssembler {

    private PatchInstallmentInputAssembler() {
    }

    /**
     * Converts the patch input into a domain {@link InstallmentPatch}.
     *
     * @param input    the request fields to apply
     * @param existing the current installment — used to resolve the currency for monetary fields
     */
    public static InstallmentPatch toPatch(PatchInstallmentInput input, Installment existing) {
        Currency currency = existing.getOriginalValue().currency();

        return InstallmentPatch.empty()
                .withDetails(input.details())
                .withFlag(input.flag())
                .withOriginalValue(input.originalValue() == null ? null : Money.of(input.originalValue(), currency))
                .withInstallmentValue(input.installmentValue() == null ? null : Money.of(input.installmentValue(), currency))
                .withInstallmentNumber(input.installmentNumber())
                .withSourceEffectiveMonth(input.sourceEffectiveMonth())
                .withPurchaseDate(input.purchaseDate())
                .withCreditCardId(input.creditCardId());
    }
}
