package br.com.casellisoftware.budgetmanager.persistence.expense.mappers;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import org.springframework.stereotype.Component;

import java.util.Currency;

/**
 * Manual mapper between {@link Expense} (rich domain entity) and
 * {@link ExpenseDocument} (Mongo persistence model). MapStruct is not used here
 * because the domain is built through private factories ({@link Expense#rehydrate})
 * and uses the {@link Money} value object.
 */
@Component
public class ExpensePersistenceMapper {

    public ExpenseDocument toDocument(Expense expense) {
        ExpenseDocument document = new ExpenseDocument();
        document.setId(expense.getId());
        document.setName(expense.getName());
        document.setCost(expense.getCost().amount());
        document.setRemaining(expense.getRemaining().amount());
        document.setCurrency(expense.getCost().currency().getCurrencyCode());
        document.setPurchaseDate(expense.getPurchaseDate());
        document.setWalletId(expense.getWalletId());
        return document;
    }

    public Expense toDomain(ExpenseDocument document) {
        Currency currency = document.getCurrency() == null
                ? Money.DEFAULT_CURRENCY
                : Currency.getInstance(document.getCurrency());
        Money cost = Money.of(document.getCost(), currency);
        Money remaining = Money.of(document.getRemaining(), currency);
        return Expense.rehydrate(
                document.getId(),
                document.getWalletId(),
                document.getName(),
                cost,
                remaining,
                document.getPurchaseDate()
        );
    }
}
