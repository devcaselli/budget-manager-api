package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Currency;

/**
 * MapStruct mapper between {@link Wallet} and {@link WalletDocument}.
 *
 * <p>{@code toDocument} flattens {@link Money} into amount + currency fields,
 * matching the Mongo shape used by expenses and bullets. {@code toDomain} is a
 * default method because reconstructing {@link Money} from separate fields
 * requires building {@link Currency} instances.</p>
 */
@Mapper(config = ProjectMapper.class)
public interface WalletPersistenceMapper {

    @Mapping(source = "closed", target = "isClosed")
    @Mapping(source = "budget.amount", target = "budgetAmount")
    @Mapping(target = "budgetCurrency", expression = "java(wallet.getBudget().currency().getCurrencyCode())")
    @Mapping(source = "remaining.amount", target = "remainingAmount")
    @Mapping(target = "remainingCurrency", expression = "java(wallet.getRemaining().currency().getCurrencyCode())")
    @Mapping(target = "version", ignore = true)
    WalletDocument toDocument(Wallet wallet);

    @Mapping(source = "wallet.closed", target = "isClosed")
    @Mapping(source = "wallet.budget.amount", target = "budgetAmount")
    @Mapping(target = "budgetCurrency", expression = "java(wallet.getBudget().currency().getCurrencyCode())")
    @Mapping(source = "wallet.remaining.amount", target = "remainingAmount")
    @Mapping(target = "remainingCurrency", expression = "java(wallet.getRemaining().currency().getCurrencyCode())")
    @Mapping(target = "version", source = "version")
    WalletDocument toDocument(Wallet wallet, Long version);

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
