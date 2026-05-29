package br.com.casellisoftware.budgetmanager.persistence.extrabudget.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.extrabudget.AllocationSubDocument;
import br.com.casellisoftware.budgetmanager.persistence.extrabudget.ExtraBudgetDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.List;

/**
 * MapStruct mapper between {@link ExtraBudget} and {@link ExtraBudgetDocument}.
 *
 * <p>Currency is stored once on the parent document; allocations inherit it.
 * {@code toDomain} is a default method to handle currency reconstruction and
 * the allocation sub-document list mapping.</p>
 */
@Mapper(config = ProjectMapper.class)
public interface ExtraBudgetPersistenceMapper {

    Logger LOG = LoggerFactory.getLogger(ExtraBudgetPersistenceMapper.class);

    @BeanMapping(ignoreUnmappedSourceProperties = {"allocations"})
    @Mapping(target = "amount", source = "extraBudget.amount.amount")
    @Mapping(target = "currency", expression = "java(extraBudget.getAmount().currency().getCurrencyCode())")
    @Mapping(target = "allocations", expression = "java(toSubDocuments(extraBudget.getAllocations()))")
    @Mapping(target = "version", ignore = true)
    ExtraBudgetDocument toDocument(ExtraBudget extraBudget);

    @BeanMapping(ignoreUnmappedSourceProperties = {"allocations"})
    @Mapping(target = "amount", source = "extraBudget.amount.amount")
    @Mapping(target = "currency", expression = "java(extraBudget.getAmount().currency().getCurrencyCode())")
    @Mapping(target = "allocations", expression = "java(toSubDocuments(extraBudget.getAllocations()))")
    @Mapping(target = "version", source = "version")
    ExtraBudgetDocument toDocument(ExtraBudget extraBudget, Long version);

    default List<AllocationSubDocument> toSubDocuments(List<ExtraBudgetAllocation> allocations) {
        return allocations.stream()
                .map(a -> new AllocationSubDocument(a.bulletId(), a.amount().amount()))
                .toList();
    }

    default ExtraBudget toDomain(ExtraBudgetDocument document) {
        Currency currency;
        if (document.getCurrency() == null) {
            LOG.warn("ExtraBudgetDocument id={} has no currency — falling back to {}",
                    document.getId(), Money.DEFAULT_CURRENCY);
            currency = Money.DEFAULT_CURRENCY;
        } else {
            currency = Currency.getInstance(document.getCurrency());
        }

        List<ExtraBudgetAllocation> allocations = document.getAllocations().stream()
                .map(a -> new ExtraBudgetAllocation(a.getBulletId(), Money.of(a.getAmount(), currency)))
                .toList();

        return ExtraBudget.rebuild(
                document.getId(),
                document.getOwnerId(),
                document.getDescription(),
                document.getWalletId(),
                Money.of(document.getAmount(), currency),
                allocations,
                document.isDeleted(),
                document.getDeletedAt()
        );
    }

}
