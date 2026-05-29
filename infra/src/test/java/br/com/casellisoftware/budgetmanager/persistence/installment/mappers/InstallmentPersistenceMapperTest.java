package br.com.casellisoftware.budgetmanager.persistence.installment.mappers;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.installment.InstallmentDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentPersistenceMapperTest {

    private final InstallmentPersistenceMapper mapper = Mappers.getMapper(InstallmentPersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndLeavesVersionNull() {
        Installment installment = Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        InstallmentDocument document = mapper.toDocument(installment);

        assertThat(document.getId()).isEqualTo(installment.getId());
        assertThat(document.getVersion()).isNull();
        assertThat(document.getDescription()).isEqualTo("Notebook");
        assertThat(document.getOriginalAmount()).isEqualByComparingTo("6000.00");
        assertThat(document.getInstallmentAmount()).isEqualByComparingTo("1000.00");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getInstallmentNumber()).isEqualTo(6);
        assertThat(document.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(document.getLastInstallmentDate()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(document.getCreditCardId()).isEqualTo("cc1");
        assertThat(document.getSourceExpenseId()).isNull();
        assertThat(document.getSourceWalletId()).isEqualTo("w1");
        assertThat(document.getSourceEffectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(document.isDeleted()).isFalse();
        assertThat(document.getDeletedAt()).isNull();
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
    }

    @Test
    void toDocument_withVersionCopiesVersion() {
        Installment installment = Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );

        InstallmentDocument document = mapper.toDocument(installment, 3L);

        assertThat(document.getVersion()).isEqualTo(3L);
    }

    @Test
    void toDomain_copiesAllFields() {
        InstallmentDocument document = new InstallmentDocument(
                "id-1",
                null,
                "Notebook",
                new BigDecimal("6000.00"),
                new BigDecimal("1000.00"),
                "BRL",
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                "exp-1",
                "w1",
                YearMonth.of(2026, 5),
                true,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                FlagEnum.NONE
        );

        Installment installment = mapper.toDomain(document);

        assertThat(installment.getId()).isEqualTo("id-1");
        assertThat(installment.getDescription()).isEqualTo("Notebook");
        assertThat(installment.getOriginalValue().amount()).isEqualByComparingTo("6000.00");
        assertThat(installment.getInstallmentValue().amount()).isEqualByComparingTo("1000.00");
        assertThat(installment.getOriginalValue().currency().getCurrencyCode()).isEqualTo("BRL");
        assertThat(installment.getInstallmentNumber()).isEqualTo(6);
        assertThat(installment.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(installment.getLastInstallmentDate()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(installment.getCreditCardId()).isEqualTo("cc1");
        assertThat(installment.getSourceExpenseId()).isEqualTo("exp-1");
        assertThat(installment.getSourceWalletId()).isEqualTo("w1");
        assertThat(installment.getSourceEffectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(installment.isDeleted()).isTrue();
        assertThat(installment.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 6, 15, 10, 0));
    }

    @Test
    void toDomain_nullFlag_defaultsToNone() {
        InstallmentDocument document = new InstallmentDocument(
                "id-1",
                null,
                "X",
                new BigDecimal("600.00"),
                new BigDecimal("100.00"),
                "BRL",
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                null,
                "w1",
                YearMonth.of(2026, 5),
                false,
                null,
                null
        );

        assertThat(mapper.toDomain(document).getFlag()).isEqualTo(FlagEnum.NONE);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Installment original = Installment.rebuild(
                "id-1",
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                true,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        Installment roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void toDocument_whenInstallmentIsNull_returnsNull() {
        assertThat(mapper.toDocument(null)).isNull();
        assertThat(mapper.toDocument(null, 1L)).isNull();
    }

    @Test
    void toDomain_whenDocumentIsNull_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
