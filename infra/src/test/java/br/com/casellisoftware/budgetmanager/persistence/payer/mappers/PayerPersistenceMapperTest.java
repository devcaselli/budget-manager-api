package br.com.casellisoftware.budgetmanager.persistence.payer.mappers;

import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.persistence.payer.PayerDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PayerPersistenceMapperTest {

    private final PayerPersistenceMapper mapper = Mappers.getMapper(PayerPersistenceMapper.class);

    @Test
    void toDocument_mapsDomainToDocument() {
        Payer payer = new Payer(
                "payer-1",
                "owner-1",
                "Joao",
                PayerType.STANDING,
                null,
                "sub-1",
                LocalDate.of(2026, 5, 10),
                false);

        PayerDocument document = mapper.toDocument(payer, 3L);

        assertThat(document.getId()).isEqualTo("payer-1");
        assertThat(document.getOwnerId()).isEqualTo("owner-1");
        assertThat(document.getType()).isEqualTo("STANDING");
        assertThat(document.getVersion()).isEqualTo(3L);
    }

    @Test
    void toDomain_mapsDocumentToDomainAndDefaultsLegacyOwner() {
        PayerDocument document = new PayerDocument(
                "payer-1",
                null,
                "Joao",
                "TRANSIENT",
                "wallet-1",
                null,
                LocalDate.of(2026, 5, 10),
                true,
                1L);

        Payer payer = mapper.toDomain(document);

        assertThat(payer.getOwnerId()).isEqualTo(Payer.LEGACY_OWNER_ID);
        assertThat(payer.getType()).isEqualTo(PayerType.TRANSIENT);
        assertThat(payer.getWalletId()).isEqualTo("wallet-1");
        assertThat(payer.isDeleted()).isTrue();
    }
}
