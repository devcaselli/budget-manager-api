package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionPatch;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class PatchSubscriptionInputAssemblerTest {

    @Test
    void toPatch_mapsOnlyProvidedFieldsUsingSubscriptionCurrency() {
        PatchSubscriptionInput input = new PatchSubscriptionInput(
                "subscription-1",
                "music",
                new BigDecimal("65.00"),
                "cc-9"
        );

        SubscriptionPatch patch = PatchSubscriptionInputAssembler.toPatch(input, Currency.getInstance("BRL"));

        assertThat(patch.description()).contains("music");
        assertThat(patch.newAmount()).contains(Money.of("65.00"));
        assertThat(patch.creditCardId()).contains("cc-9");
    }

    @Test
    void toPatch_keepsNullFieldsEmpty() {
        PatchSubscriptionInput input = new PatchSubscriptionInput("subscription-1", null, null);

        SubscriptionPatch patch = PatchSubscriptionInputAssembler.toPatch(input, Currency.getInstance("BRL"));

        assertThat(patch.isEmpty()).isTrue();
    }
}
