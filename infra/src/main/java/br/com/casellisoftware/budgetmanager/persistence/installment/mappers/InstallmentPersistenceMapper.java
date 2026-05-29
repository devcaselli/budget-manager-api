package br.com.casellisoftware.budgetmanager.persistence.installment.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.installment.InstallmentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;

@Mapper(config = ProjectMapper.class)
public interface InstallmentPersistenceMapper {

    default InstallmentDocument toDocument(Installment installment) {
        if (installment == null) {
            return null;
        }
        return toDocumentInternal(installment);
    }

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "originalAmount", source = "installment.originalValue.amount")
    @Mapping(target = "installmentAmount", source = "installment.installmentValue.amount")
    @Mapping(target = "currency", expression = "java(installment.getOriginalValue().currency().getCurrencyCode())")
    @Mapping(target = "deleted", source = "installment.deleted")
    InstallmentDocument toDocumentInternal(Installment installment);

    default InstallmentDocument toDocument(Installment installment, Long version) {
        if (installment == null) {
            return null;
        }
        return toDocumentInternal(installment, version);
    }

    @Mapping(target = "version", source = "version")
    @Mapping(target = "originalAmount", source = "installment.originalValue.amount")
    @Mapping(target = "installmentAmount", source = "installment.installmentValue.amount")
    @Mapping(target = "currency", expression = "java(installment.getOriginalValue().currency().getCurrencyCode())")
    @Mapping(target = "deleted", source = "installment.deleted")
    InstallmentDocument toDocumentInternal(Installment installment, Long version);

    default Installment toDomain(InstallmentDocument document) {
        if (document == null) {
            return null;
        }

        Currency currency;
        if (document.getCurrency() == null) {
            log().warn("InstallmentDocument id={} has no currency — falling back to {}",
                    document.getId(), Money.DEFAULT_CURRENCY);
            currency = Money.DEFAULT_CURRENCY;
        } else {
            currency = Currency.getInstance(document.getCurrency());
        }

        return Installment.rebuild(
                document.getId(),
                document.getDescription(),
                document.getDetails(),
                Money.of(document.getOriginalAmount(), currency),
                Money.of(document.getInstallmentAmount(), currency),
                document.getInstallmentNumber(),
                document.getPurchaseDate(),
                document.getLastInstallmentDate(),
                document.getCreditCardId(),
                document.getSourceExpenseId(),
                document.getSourceWalletId(),
                document.getSourceEffectiveMonth(),
                document.isDeleted(),
                document.getDeletedAt(),
                document.getFlag() == null ? FlagEnum.NONE : document.getFlag(),
                document.getOwnerId() == null ? Installment.LEGACY_OWNER_ID : document.getOwnerId()
        );
    }

    private static Logger log() {
        return LoggerFactory.getLogger(InstallmentPersistenceMapper.class);
    }
}
