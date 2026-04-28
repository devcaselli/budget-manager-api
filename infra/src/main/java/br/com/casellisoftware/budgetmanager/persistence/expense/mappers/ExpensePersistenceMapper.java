package br.com.casellisoftware.budgetmanager.persistence.expense.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

/**
 * MapStruct mapper between {@link Expense} and {@link ExpenseDocument}.
 *
 * <p>{@code toDocument} is fully generated — MapStruct flattens {@link Money}
 * into {@code BigDecimal} + {@code String} fields. {@code toDomain} is a default
 * method because reconstructing {@link Money} from two separate document fields
 * (amount + currency) with a fallback requires logic that MapStruct's declarative
 * model doesn't express cleanly.</p>
 */
@Mapper(config = ProjectMapper.class)
public interface ExpensePersistenceMapper {

    @Mapping(target = "cost", source = "expense.cost.amount")
    @Mapping(target = "remaining", source = "expense.remaining.amount")
    @Mapping(target = "currency", expression = "java(expense.getCost().currency().getCurrencyCode())")
    @Mapping(target = "version", ignore = true)
    ExpenseDocument toDocument(Expense expense);

    @Mapping(target = "cost", source = "expense.cost.amount")
    @Mapping(target = "remaining", source = "expense.remaining.amount")
    @Mapping(target = "currency", expression = "java(expense.getCost().currency().getCurrencyCode())")
    @Mapping(target = "version", source = "version")
    ExpenseDocument toDocument(Expense expense, Long version);

    default Expense toDomain(ExpenseDocument document) {
        Currency currency;
        if (document.getCurrency() == null) {
            log().warn("Document id={} has no currency — falling back to {}",
                    document.getId(), Money.DEFAULT_CURRENCY);
            currency = Money.DEFAULT_CURRENCY;
        } else {
            currency = Currency.getInstance(document.getCurrency());
        }
        return new Expense(
                document.getId(),
                document.getWalletId(),
                document.getName(),
                Money.of(document.getCost(), currency),
                Money.of(document.getRemaining(), currency),
                document.getPurchaseDate(),
                document.getPaymentIds()
        );
    }

    private static Logger log() {
        return LoggerFactory.getLogger(ExpensePersistenceMapper.class);
    }
}
