package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.YearMonth;
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
    @Mapping(source = "effectiveMonth", target = "effectiveMonth")
    @Mapping(source = "state", target = "state", qualifiedByName = "walletStateToString")
    WalletDocument toDocument(Wallet wallet);

    @Mapping(source = "wallet.closed", target = "isClosed")
    @Mapping(source = "wallet.budget.amount", target = "budgetAmount")
    @Mapping(target = "budgetCurrency", expression = "java(wallet.getBudget().currency().getCurrencyCode())")
    @Mapping(source = "wallet.remaining.amount", target = "remainingAmount")
    @Mapping(target = "remainingCurrency", expression = "java(wallet.getRemaining().currency().getCurrencyCode())")
    @Mapping(target = "version", source = "version")
    @Mapping(source = "wallet.effectiveMonth", target = "effectiveMonth")
    @Mapping(source = "wallet.state", target = "state", qualifiedByName = "walletStateToString")
    WalletDocument toDocument(Wallet wallet, Long version);

    @Named("walletStateToString")
    default String walletStateToString(WalletState state) {
        return state == null ? null : state.name();
    }

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

        YearMonth effectiveMonth = document.getEffectiveMonth();
        if (effectiveMonth == null && document.getStartDate() != null) {
            effectiveMonth = YearMonth.from(document.getStartDate());
        }
        WalletState state = document.getState() == null
                ? WalletState.PRODUCTION
                : WalletState.valueOf(document.getState());

        return new Wallet(
                document.getId(),
                document.getOwnerId() == null ? Wallet.LEGACY_OWNER_ID : document.getOwnerId(),
                document.getDescription(),
                budget,
                remaining,
                document.getStartDate(),
                document.getClosedDate(),
                document.getIsClosed(),
                effectiveMonth,
                state,
                document.getFlag() == null ? FlagEnum.NONE : document.getFlag()
        );
    }
}
