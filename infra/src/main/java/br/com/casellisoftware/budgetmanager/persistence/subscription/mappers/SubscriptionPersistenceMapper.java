package br.com.casellisoftware.budgetmanager.persistence.subscription.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.persistence.subscription.SubscriptionDocument;
import br.com.casellisoftware.budgetmanager.persistence.subscription.SubscriptionVersionDocument;
import org.mapstruct.Mapper;

import java.util.Currency;
import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface SubscriptionPersistenceMapper {

    default SubscriptionDocument toDocument(Subscription subscription) {
        return toDocument(subscription, null);
    }

    default SubscriptionDocument toDocument(Subscription subscription, Long version) {
        if (subscription == null) {
            return null;
        }

        List<SubscriptionVersionDocument> versions = subscription.getVersions()
                .stream()
                .map(this::toDocument)
                .toList();

        return new SubscriptionDocument(
                subscription.getId(),
                subscription.getOwnerId(),
                version,
                subscription.getDescription(),
                subscription.getCurrency().getCurrencyCode(),
                subscription.getState().name(),
                subscription.getFlag(),
                subscription.getStartMonth(),
                subscription.getEndMonth(),
                versions,
                subscription.getCreditCardId()
        );
    }

    default Subscription toDomain(SubscriptionDocument document) {
        if (document == null) {
            return null;
        }

        Currency currency = Currency.getInstance(document.getCurrency());
        List<SubscriptionVersion> versions = requireVersions(document)
                .stream()
                .map(version -> toDomain(version, currency))
                .toList();

        return Subscription.rebuild(
                document.getId(),
                document.getDescription(),
                currency,
                document.getStartMonth(),
                document.getEndMonth(),
                resolveState(document),
                versions,
                document.getFlag() == null ? FlagEnum.NONE : document.getFlag(),
                document.getOwnerId() == null ? Subscription.LEGACY_OWNER_ID : document.getOwnerId(),
                document.getCreditCardId()
        );
    }

    default SubscriptionVersionDocument toDocument(SubscriptionVersion version) {
        if (version == null) {
            return null;
        }
        return new SubscriptionVersionDocument(version.effectiveMonth(), version.amount().amount());
    }

    default SubscriptionVersion toDomain(SubscriptionVersionDocument document, Currency currency) {
        if (document == null) {
            return null;
        }
        return new SubscriptionVersion(
                document.getEffectiveMonth(),
                Money.of(document.getAmount(), currency)
        );
    }

    private static List<SubscriptionVersionDocument> requireVersions(SubscriptionDocument document) {
        List<SubscriptionVersionDocument> versions = document.getVersions();
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException(
                    "subscription document versions must not be null or empty: id=" + document.getId());
        }
        return versions;
    }

    private static SubscriptionState resolveState(SubscriptionDocument document) {
        String state = document.getState();
        return state == null ? SubscriptionState.PRODUCTION : SubscriptionState.valueOf(state);
    }
}
