package br.com.casellisoftware.budgetmanager.persistence.payment.mappers;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PaymentPersistenceMapper {

    PaymentPersistenceMapper INSTANCE = Mappers.getMapper(PaymentPersistenceMapper.class);

    PaymentDocument toDocument(Payment payment);

    @Mapping(source = "paymentDate", target = "paymentDate")
    Payment toDomain(PaymentDocument document);
}