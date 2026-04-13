package br.com.casellisoftware.budgetmanager.rest.payment.mappers;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentInput;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import org.mapstruct.Mapper;

@Mapper
public interface PaymentRestMapper {


    PaymentInput paymentDomainToPaymentInput(Payment payment);
}
