package br.com.casellisoftware.budgetmanager.persistence.expense.mappers;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpensePersistenceMapperTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(2);
    private static final String CREDIT_CARD_ID = "cc-1";

    private final ExpensePersistenceMapper mapper = Mappers.getMapper(ExpensePersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndCurrency() {
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "lunch", Money.of("10.50"), PURCHASE_DATE,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, true);

        ExpenseDocument document = mapper.toDocument(expense);

        assertThat(document.getId()).isEqualTo(expense.getId());
        assertThat(document.getName()).isEqualTo("lunch");
        assertThat(document.getCost()).isEqualByComparingTo("10.50");
        assertThat(document.getRemaining()).isEqualByComparingTo("10.50");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(document.getWalletId()).isEqualTo("wallet-1");
        assertThat(document.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        assertThat(document.isHidden()).isTrue();
        assertThat(document.getVersion()).isNull();
    }

    @Test
    void toDomain_copiesAllFields() {
        ExpenseDocument document = new ExpenseDocument(
                "id-1",
                null,
                "coffee",
                new BigDecimal("5.00"),
                new BigDecimal("2.50"),
                "BRL",
                PURCHASE_DATE,
                "wallet-2",
                CREDIT_CARD_ID,
                List.of("payment-1", "payment-2"),
                null
        );
        document.setFlag(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);

        Expense expense = mapper.toDomain(document);

        assertThat(expense.getId()).isEqualTo("id-1");
        assertThat(expense.getName()).isEqualTo("coffee");
        assertThat(expense.getCost().amount()).isEqualByComparingTo("5.00");
        assertThat(expense.getCost().currency().getCurrencyCode()).isEqualTo("BRL");
        assertThat(expense.getRemaining().amount()).isEqualByComparingTo("2.50");
        assertThat(expense.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(expense.getWalletId()).isEqualTo("wallet-2");
        assertThat(expense.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(expense.getPaymentIds()).containsExactly("payment-1", "payment-2");
        assertThat(expense.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        assertThat(expense.isHidden()).isFalse();
    }

    @Test
    void toDomain_whenCurrencyMissing_fallsBackToDefault() {
        ExpenseDocument document = new ExpenseDocument(
                "id-2",
                null,
                "legacy",
                new BigDecimal("1.00"),
                new BigDecimal("1.00"),
                null,
                PURCHASE_DATE,
                "wallet-legacy",
                CREDIT_CARD_ID,
                null,
                null
        );

        Expense expense = mapper.toDomain(document);

        assertThat(expense.getCost().currency()).isEqualTo(Money.DEFAULT_CURRENCY);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Expense original = Expense.create("wallet-1", CREDIT_CARD_ID, "dinner", Money.of("42.00"), PURCHASE_DATE,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);

        Expense roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
