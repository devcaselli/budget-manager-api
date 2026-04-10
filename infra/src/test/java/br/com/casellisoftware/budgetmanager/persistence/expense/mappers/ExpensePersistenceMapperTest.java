package br.com.casellisoftware.budgetmanager.persistence.expense.mappers;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpensePersistenceMapperTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(2);

    private final ExpensePersistenceMapper mapper = Mappers.getMapper(ExpensePersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndCurrency() {
        Expense expense = Expense.create("wallet-1", "lunch", Money.of("10.50"), PURCHASE_DATE);

        ExpenseDocument document = mapper.toDocument(expense);

        assertThat(document.getId()).isEqualTo(expense.getId());
        assertThat(document.getName()).isEqualTo("lunch");
        assertThat(document.getCost()).isEqualByComparingTo("10.50");
        assertThat(document.getRemaining()).isEqualByComparingTo("10.50");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(document.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void toDomain_copiesAllFields() {
        ExpenseDocument document = new ExpenseDocument(
                "id-1",
                "coffee",
                new BigDecimal("5.00"),
                new BigDecimal("2.50"),
                "BRL",
                PURCHASE_DATE,
                "wallet-2"
        );

        Expense expense = mapper.toDomain(document);

        assertThat(expense.getId()).isEqualTo("id-1");
        assertThat(expense.getName()).isEqualTo("coffee");
        assertThat(expense.getCost().amount()).isEqualByComparingTo("5.00");
        assertThat(expense.getCost().currency().getCurrencyCode()).isEqualTo("BRL");
        assertThat(expense.getRemaining().amount()).isEqualByComparingTo("2.50");
        assertThat(expense.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(expense.getWalletId()).isEqualTo("wallet-2");
    }

    @Test
    void toDomain_whenCurrencyMissing_fallsBackToDefault() {
        ExpenseDocument document = new ExpenseDocument(
                "id-2",
                "legacy",
                new BigDecimal("1.00"),
                new BigDecimal("1.00"),
                null,
                PURCHASE_DATE,
                "wallet-legacy"
        );

        Expense expense = mapper.toDomain(document);

        assertThat(expense.getCost().currency()).isEqualTo(Money.DEFAULT_CURRENCY);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Expense original = Expense.create("wallet-1", "dinner", Money.of("42.00"), PURCHASE_DATE);

        Expense roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
