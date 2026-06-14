package br.com.casellisoftware.budgetmanager.persistence.reservedbudget.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetVersion;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.ReservedBudgetDocument;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.ReservedBudgetLinkDocument;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.ReservedBudgetVersionDocument;
import org.mapstruct.Mapper;

import java.util.Currency;
import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface ReservedBudgetPersistenceMapper {

    default ReservedBudgetDocument toDocument(ReservedBudget reservedBudget) {
        return toDocument(reservedBudget, null);
    }

    default ReservedBudgetDocument toDocument(ReservedBudget reservedBudget, Long version) {
        if (reservedBudget == null) {
            return null;
        }

        List<ReservedBudgetVersionDocument> versions = reservedBudget.getVersions()
                .stream()
                .map(this::toDocument)
                .toList();

        List<ReservedBudgetLinkDocument> links = reservedBudget.getLinks()
                .stream()
                .map(this::toDocument)
                .toList();

        return new ReservedBudgetDocument(
                reservedBudget.getId(),
                reservedBudget.getOwnerId(),
                version,
                reservedBudget.getDescription(),
                reservedBudget.getDetails(),
                reservedBudget.getCurrency().getCurrencyCode(),
                reservedBudget.getFlag(),
                reservedBudget.getStartMonth(),
                reservedBudget.isDeleted(),
                reservedBudget.getDeletedAt(),
                versions,
                links
        );
    }

    default ReservedBudget toDomain(ReservedBudgetDocument document) {
        if (document == null) {
            return null;
        }

        Currency currency = Currency.getInstance(document.getCurrency());
        List<ReservedBudgetVersion> versions = requireVersions(document)
                .stream()
                .map(version -> toDomain(version, currency))
                .toList();

        List<ReservedBudgetLink> links = (document.getLinks() == null)
                ? List.of()
                : document.getLinks().stream()
                        .map(this::toDomain)
                        .toList();

        return ReservedBudget.rebuild(
                document.getId(),
                document.getOwnerId() == null ? ReservedBudget.LEGACY_OWNER_ID : document.getOwnerId(),
                document.getDescription(),
                document.getDetails(),
                currency,
                document.getStartMonth(),
                versions,
                links,
                document.isDeleted(),
                document.getDeletedAt(),
                document.getFlag() == null ? FlagEnum.NONE : document.getFlag()
        );
    }

    default ReservedBudgetVersionDocument toDocument(ReservedBudgetVersion version) {
        if (version == null) {
            return null;
        }
        return new ReservedBudgetVersionDocument(version.effectiveMonth(), version.amount().amount());
    }

    default ReservedBudgetVersion toDomain(ReservedBudgetVersionDocument document, Currency currency) {
        if (document == null) {
            return null;
        }
        return new ReservedBudgetVersion(
                document.getEffectiveMonth(),
                Money.of(document.getAmount(), currency)
        );
    }

    default ReservedBudgetLinkDocument toDocument(ReservedBudgetLink link) {
        if (link == null) {
            return null;
        }
        return new ReservedBudgetLinkDocument(link.sourceType().name(), link.sourceId(), link.fromMonth());
    }

    default ReservedBudgetLink toDomain(ReservedBudgetLinkDocument document) {
        if (document == null) {
            return null;
        }
        return new ReservedBudgetLink(
                ReservedBudgetLinkSourceType.valueOf(document.getSourceType()),
                document.getSourceId(),
                document.getFromMonth()
        );
    }

    private static List<ReservedBudgetVersionDocument> requireVersions(ReservedBudgetDocument document) {
        List<ReservedBudgetVersionDocument> versions = document.getVersions();
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException(
                    "reserved-budget document versions must not be null or empty: id=" + document.getId());
        }
        return versions;
    }
}
