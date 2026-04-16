package br.com.casellisoftware.budgetmanager.persistence.payment.mappers;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR
)
public interface PaymentPersistenceMapper {

    PaymentDocument toDocument(Payment payment);

    @Mapping(source = "paymentDate", target = "paymentDate")
    Payment toDomain(PaymentDocument document);
}