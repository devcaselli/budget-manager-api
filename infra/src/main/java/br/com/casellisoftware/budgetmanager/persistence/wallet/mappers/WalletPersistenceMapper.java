package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Currency;

/**
 * MapStruct mapper between {@link Wallet} and {@link WalletDocument}.
 *
 * <p>{@code toDocument} flattens {@link Money} into amount + currency fields,
 * matching the Mongo shape used by expenses and bullets. {@code toDomain} is a
 * default method because reconstructing {@link Money} from separate fields
 * requires building {@link Currency} instances.</p>
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR)
public interface WalletPersistenceMapper {

    @Mapping(source = "closed", target = "isClosed")
    @Mapping(source = "budget.amount", target = "budgetAmount")
    @Mapping(target = "budgetCurrency", expression = "java(wallet.getBudget().currency().getCurrencyCode())")
    @Mapping(source = "remaining.amount", target = "remainingAmount")
    @Mapping(target = "remainingCurrency", expression = "java(wallet.getRemaining().currency().getCurrencyCode())")
    @Mapping(target = "version", ignore = true)
    WalletDocument toDocument(Wallet wallet);

    default Wallet toDomain(WalletDocument document) {
        if (document == null) {
            return null;
        }

        Money budget = Money.of(
                document.getBudgetAmount(),
                Currency.getInstance(document.getBudgetCurrency())
        );
        Money remaining = Money.of(
                document.getRemainingAmount(),
                Currency.getInstance(document.getRemainingCurrency())
        );

        return new Wallet(
                document.getId(),
                document.getDescription(),
                budget,
                remaining,
                document.getStartDate(),
                document.getClosedDate(),
                document.getIsClosed()
        );
    }
}
