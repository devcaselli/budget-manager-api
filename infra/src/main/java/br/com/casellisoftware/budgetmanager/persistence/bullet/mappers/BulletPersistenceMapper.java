package br.com.casellisoftware.budgetmanager.persistence.bullet.mappers;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.bullet.BulletDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

/**
 * MapStruct mapper between {@link Bullet} and {@link BulletDocument}.
 *
 * <p>{@code toDocument} is fully generated — MapStruct flattens {@link Money}
 * into {@code BigDecimal} + {@code String} fields. {@code toDomain} is a default
 * method because reconstructing {@link Money} from two separate document fields
 * (amount + currency) with a fallback requires logic that MapStruct's declarative
 * model doesn't express cleanly.</p>
 */
@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BulletPersistenceMapper {

    Logger log = LoggerFactory.getLogger(BulletPersistenceMapper.class);

    @Mapping(target = "budget", source = "budget.amount")
    @Mapping(target = "remaining", source = "remaining.amount")
    @Mapping(target = "currency", expression = "java(bullet.getBudget().currency().getCurrencyCode())")
    BulletDocument toDocument(Bullet bullet);

    default Bullet toDomain(BulletDocument document) {
        Currency currency;
        if (document.getCurrency() == null) {
            log.warn("Document id={} has no currency — falling back to {}",
                    document.getId(), Money.DEFAULT_CURRENCY);
            currency = Money.DEFAULT_CURRENCY;
        } else {
            currency = Currency.getInstance(document.getCurrency());
        }
        return new Bullet(
                document.getId(),
                document.getDescription(),
                Money.of(document.getBudget(), currency),
                Money.of(document.getRemaining(), currency),
                document.getWalletId()
        );
    }
}
