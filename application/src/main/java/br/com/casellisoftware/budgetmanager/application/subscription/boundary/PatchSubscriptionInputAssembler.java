package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionPatch;

import java.util.Currency;

/**
 * Bridges the application patch contract to the domain-native
 * {@link SubscriptionPatch}, keeping the aggregate free from application DTO
 * dependencies.
 */
public final class PatchSubscriptionInputAssembler {

    private PatchSubscriptionInputAssembler() {
    }

    public static SubscriptionPatch toPatch(PatchSubscriptionInput input, Currency currency) {
        return SubscriptionPatch.empty()
                .withDescription(input.description())
                .withNewAmount(input.newAmount() == null ? null : Money.of(input.newAmount(), currency))
                .withCreditCardId(input.creditCardId())
                .withFlag(input.flag());
    }
}
