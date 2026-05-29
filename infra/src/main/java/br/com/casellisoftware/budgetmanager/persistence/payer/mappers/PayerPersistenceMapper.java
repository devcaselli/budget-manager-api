package br.com.casellisoftware.budgetmanager.persistence.payer.mappers;

import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.persistence.payer.PayerDocument;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ProjectMapper.class)
public interface PayerPersistenceMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "type", expression = "java(payer.getType().name())")
    @BeanMapping(ignoreUnmappedSourceProperties = "type")
    PayerDocument toDocument(Payer payer);

    @Mapping(target = "version", source = "version")
    @Mapping(target = "type", expression = "java(payer.getType().name())")
    @BeanMapping(ignoreUnmappedSourceProperties = "type")
    PayerDocument toDocument(Payer payer, Long version);

    default Payer toDomain(PayerDocument document) {
        if (document == null) {
            return null;
        }
        return new Payer(
                document.getId(),
                document.getOwnerId() == null ? Payer.LEGACY_OWNER_ID : document.getOwnerId(),
                document.getName(),
                PayerType.valueOf(document.getType()),
                document.getWalletId(),
                document.getSubscriptionId(),
                document.getPaymentDate(),
                document.isDeleted()
        );
    }
}
