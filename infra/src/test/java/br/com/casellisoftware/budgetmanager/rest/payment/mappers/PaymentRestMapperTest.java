package br.com.casellisoftware.budgetmanager.rest.payment.mappers;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PaymentRequestDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRestMapperTest {

    private static final Instant PAYMENT_DATE = Instant.parse("2026-04-10T12:00:00Z");

    private final PaymentRestMapper mapper = Mappers.getMapper(PaymentRestMapper.class);

    @Test
    void toPayExpenseInput_copiesAllFieldsAndBuildsMoney() {
        PayRequestDto request = new PayRequestDto(
                new PaymentRequestDto(new BigDecimal("10.50"), "USD", PAYMENT_DATE, "coffee"),
                "bullet-1",
                "expense-1"
        );

        PayExpenseInput input = mapper.toPayExpenseInput(request, "wallet-1");

        assertThat(input.amount()).isEqualTo(Money.of(new BigDecimal("10.50"), Currency.getInstance("USD")));
        assertThat(input.paymentDate()).isEqualTo(PAYMENT_DATE);
        assertThat(input.details()).isEqualTo("coffee");
        assertThat(input.expenseId()).isEqualTo("expense-1");
        assertThat(input.walletId()).isEqualTo("wallet-1");
        assertThat(input.bulletId()).isEqualTo("bullet-1");
    }

    @Test
    void toMoney_usesRequestCurrency() {
        PayRequestDto request = new PayRequestDto(
                new PaymentRequestDto(new BigDecimal("15.00"), "BRL", PAYMENT_DATE, "lunch"),
                "bullet-1",
                "expense-1"
        );

        assertThat(mapper.toMoney(request)).isEqualTo(Money.of("15.00"));
    }
}
