package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.SubscriptionChargeDocument;
import org.mapstruct.Mapper;

import java.util.Currency;

@Mapper(config = ProjectMapper.class)
public interface SubscriptionChargePersistenceMapper {

    default SubscriptionChargeDocument toDocument(SubscriptionCharge subscriptionCharge) {
        return toDocument(subscriptionCharge, null);
    }

    default SubscriptionChargeDocument toDocument(SubscriptionCharge subscriptionCharge, Long version) {
        if (subscriptionCharge == null) {
            return null;
        }

        return new SubscriptionChargeDocument(
                subscriptionCharge.getId(),
                subscriptionCharge.getOwnerId(),
                version,
                subscriptionCharge.getSubscriptionId(),
                subscriptionCharge.getWalletId(),
                subscriptionCharge.getMonth(),
                subscriptionCharge.getAmount().amount(),
                subscriptionCharge.getRemaining().amount(),
                subscriptionCharge.getAmount().currency().getCurrencyCode(),
                subscriptionCharge.getFlag()
        );
    }

    default SubscriptionCharge toDomain(SubscriptionChargeDocument document) {
        if (document == null) {
            return null;
        }

        Currency currency = Currency.getInstance(document.getCurrency());
        return SubscriptionCharge.rebuild(
                document.getId(),
                document.getSubscriptionId(),
                document.getWalletId(),
                document.getMonth(),
                Money.of(document.getAmount(), currency),
                Money.of(document.getRemaining(), currency),
                document.getFlag() == null ? FlagEnum.NONE : document.getFlag(),
                document.getOwnerId() == null ? SubscriptionCharge.LEGACY_OWNER_ID : document.getOwnerId()
        );
    }
}
