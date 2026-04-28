package br.com.casellisoftware.budgetmanager.persistence.payment.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

/**
 * MapStruct mapper between {@link Payment} and {@link PaymentDocument}.
 *
 * <p>{@code toDocument} is fully generated — MapStruct flattens {@link Money}
 * into {@code BigDecimal} + {@code String} fields. {@code toDomain} is a default
 * method because reconstructing {@link Money} from two separate document fields
 * (amount + currency) with a fallback requires logic that MapStruct's declarative
 * model doesn't express cleanly.</p>
 */
@Mapper(config = ProjectMapper.class)
public interface PaymentPersistenceMapper {

    @Mapping(source = "amount.amount", target = "amount")
    @Mapping(source = "amount.currency.currencyCode", target = "currency")
    PaymentDocument toDocument(Payment payment);

    default Payment toDomain(PaymentDocument document) {
        if (document == null) return null;
        Currency currency;
        if (document.getCurrency() == null) {
            log().warn("PaymentDocument id={} has no currency — falling back to {}",
                    document.getId(), Money.DEFAULT_CURRENCY);
            currency = Money.DEFAULT_CURRENCY;
        } else {
            currency = Currency.getInstance(document.getCurrency());
        }
        Money money = Money.of(document.getAmount(), currency);
        return Payment.rebuild(
                document.getId(),
                money,
                document.getPaymentDate(),
                document.getDetails(),
                document.getExpenseId(),
                document.getWalletId(),
                document.getBulletId()
        );
    }

    private static Logger log() {
        return LoggerFactory.getLogger(PaymentPersistenceMapper.class);
    }
}
