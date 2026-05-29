package br.com.casellisoftware.budgetmanager.persistence.creditcard.mappers;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.CreditCardDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCardPersistenceMapperTest {

    private final CreditCardPersistenceMapper mapper = Mappers.getMapper(CreditCardPersistenceMapper.class);

    @Test
    void toDocument_withoutVersion_copiesIdAndNameAndLeavesVersionNull() {
        CreditCard card = new CreditCard("cc-1", "Nubank");

        CreditCardDocument document = mapper.toDocument(card);

        assertThat(document.getId()).isEqualTo("cc-1");
        assertThat(document.getName()).isEqualTo("Nubank");
        assertThat(document.getVersion()).isNull();
    }

    @Test
    void toDocument_withVersion_propagatesVersion() {
        CreditCard card = new CreditCard("cc-1", "Nubank");

        CreditCardDocument document = mapper.toDocument(card, 7L);

        assertThat(document.getId()).isEqualTo("cc-1");
        assertThat(document.getName()).isEqualTo("Nubank");
        assertThat(document.getVersion()).isEqualTo(7L);
    }

    @Test
    void toDomain_buildsCreditCardFromDocument() {
        CreditCardDocument document = new CreditCardDocument("cc-1", 3L, "Itaú");

        CreditCard card = mapper.toDomain(document);

        assertThat(card.getId()).isEqualTo("cc-1");
        assertThat(card.getName()).isEqualTo("Itaú");
    }

    @Test
    void toDomain_null_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
