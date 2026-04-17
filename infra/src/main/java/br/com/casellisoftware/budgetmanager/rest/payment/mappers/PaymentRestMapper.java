package br.com.casellisoftware.budgetmanager.rest.payment.mappers;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Currency;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records
 * for the payment flow.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface PaymentRestMapper {

    @Mapping(target = "amount", expression = "java(toMoney(request))")
    @Mapping(target = "paymentDate", source = "request.payment.paymentDate")
    @Mapping(target = "details", source = "request.payment.details")
    @Mapping(target = "expenseId", source = "request.expenseId")
    @Mapping(target = "bulletId", source = "request.bulletId")
    @Mapping(target = "walletId", source = "walletId")
    PayExpenseInput toPayExpenseInput(PayRequestDto request, String walletId);

    /**
     * Composes a {@link Money} value object from the raw amount and ISO-4217
     * currency code carried in the request DTO.
     */
    default Money toMoney(PayRequestDto request) {
        Currency currency = Currency.getInstance(request.payment().currency());
        return Money.of(request.payment().amount(), currency);
    }
}
