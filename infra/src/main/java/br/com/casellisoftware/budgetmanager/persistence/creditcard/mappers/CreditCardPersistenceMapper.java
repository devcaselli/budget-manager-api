package br.com.casellisoftware.budgetmanager.persistence.creditcard.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.CreditCardDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface CreditCardPersistenceMapper {

    @Mapping(target = "version", ignore = true)
    CreditCardDocument toDocument(CreditCard creditCard);

    @Mapping(target = "version", source = "version")
    CreditCardDocument toDocument(CreditCard creditCard, Long version);

    default CreditCard toDomain(CreditCardDocument document) {
        if (document == null) return null;
        List<String> labels = document.getLabels() != null ? document.getLabels() : List.of();
        return new CreditCard(
                document.getId(),
                document.getName(),
                document.getOwnerId() == null ? CreditCard.LEGACY_OWNER_ID : document.getOwnerId(),
                labels
        );
    }
}
