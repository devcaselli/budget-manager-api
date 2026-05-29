package br.com.casellisoftware.budgetmanager.rest.payer.mappers;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PayerRestMapperTest {

    private final PayerRestMapper mapper = Mappers.getMapper(PayerRestMapper.class);

    @Test
    void payerRequestDtoToPayerInput_mapsFieldsAndLegacyOwner() {
        var input = mapper.payerRequestDtoToPayerInput(
                new PayerRequestDto("Joao", PayerType.STANDING, null, "sub-1", LocalDate.of(2026, 5, 10)));

        assertThat(input.name()).isEqualTo("Joao");
        assertThat(input.type()).isEqualTo(PayerType.STANDING);
        assertThat(input.walletId()).isNull();
        assertThat(input.subscriptionId()).isEqualTo("sub-1");
        assertThat(input.ownerId()).isEqualTo("legacy");
    }

    @Test
    void payerPatchRequestDtoToPayerPatchInput_convertsNullsToEmptyOptionals() {
        PayerPatchInput input = mapper.payerPatchRequestDtoToPayerPatchInput(
                new PayerPatchRequestDto("Maria", null, null, null, LocalDate.of(2026, 6, 15)));

        assertThat(input.name()).contains("Maria");
        assertThat(input.type()).isEmpty();
        assertThat(input.walletId()).isEmpty();
        assertThat(input.subscriptionId()).isEmpty();
        assertThat(input.paymentDate()).contains(LocalDate.of(2026, 6, 15));
    }

    @Test
    void payerOutputToPayerResponseDto_flattensMoney() {
        PayerResponseDto dto = mapper.payerOutputToPayerResponseDto(new PayerOutput(
                "payer-1",
                "Joao",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                Money.of("12.34"),
                Money.of("12.34"),
                Money.of("12.34"),
                false));

        assertThat(dto.amountDue()).isEqualByComparingTo("12.34");
        assertThat(dto.currency()).isEqualTo("BRL");
    }
}
