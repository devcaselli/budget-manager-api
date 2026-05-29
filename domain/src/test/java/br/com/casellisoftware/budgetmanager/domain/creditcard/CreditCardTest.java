package br.com.casellisoftware.budgetmanager.domain.creditcard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CreditCardTest {

    @Test
    void shouldCreateCreditCardWithValidName() {
        String name = "Visa Gold";
        CreditCard creditCard = CreditCard.create(name);

        assertThat(creditCard).isNotNull();
        assertThat(creditCard.getId()).isNotNull();
        assertThat(creditCard.getName()).isEqualTo(name);
    }

    @Test
    void shouldGenerateUniqueIdForEachCreditCard() {
        CreditCard card1 = CreditCard.create("Card 1");
        CreditCard card2 = CreditCard.create("Card 2");

        assertThat(card1.getId()).isNotEqualTo(card2.getId());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        assertThatThrownBy(() -> CreditCard.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        assertThatThrownBy(() -> CreditCard.create("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void shouldThrowExceptionWhenNameExceedsMaxLength() {
        String longName = "A".repeat(81);
        assertThatThrownBy(() -> CreditCard.create(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("80");
    }

    @Test
    void shouldAllowNameWithExactMaxLength() {
        String maxName = "A".repeat(80);
        CreditCard creditCard = CreditCard.create(maxName);

        assertThat(creditCard.getName()).hasSize(80);
    }

    @Test
    void shouldBeEqualWhenIdIsEqual() {
        CreditCard card1 = CreditCard.create("Visa");
        String id = card1.getId();
        CreditCard card2 = CreditCard.create("Mastercard");
        card2 = new CreditCard(id, "Mastercard");

        assertThat(card1).isEqualTo(card2);
    }

    @Test
    void shouldNotBeEqualWhenIdIsDifferent() {
        CreditCard card1 = CreditCard.create("Visa");
        CreditCard card2 = CreditCard.create("Visa");

        assertThat(card1).isNotEqualTo(card2);
    }

    @Test
    void shouldHaveSameHashCodeWhenIdIsEqual() {
        CreditCard card1 = CreditCard.create("Visa");
        String id = card1.getId();
        CreditCard card2 = new CreditCard(id, "Mastercard");

        assertThat(card1).hasSameHashCodeAs(card2);
    }

    @Test
    void shouldNotBeEqualToNull() {
        CreditCard creditCard = CreditCard.create("Visa");

        assertThat(creditCard).isNotEqualTo(null);
    }

    @Test
    void shouldNotBeEqualToOtherTypes() {
        CreditCard creditCard = CreditCard.create("Visa");

        assertThat(creditCard).isNotEqualTo("Visa");
    }
}
