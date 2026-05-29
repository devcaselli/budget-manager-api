package br.com.casellisoftware.budgetmanager.rest.creditcard.mappers;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardRequestDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.PagedCreditCardResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCardRestMapperTest {

    private final CreditCardRestMapper mapper = Mappers.getMapper(CreditCardRestMapper.class);

    @Test
    void creditCardRequestDtoToCreditCardInput_copiesAllFields() {
        CreditCardRequestDto dto = new CreditCardRequestDto("Nubank");

        CreditCardInput input = mapper.creditCardRequestDtoToCreditCardInput(dto);

        assertThat(input).isEqualTo(new CreditCardInput("Nubank"));
    }

    @Test
    void creditCardOutputToCreditCardResponseDto_copiesAllFields() {
        CreditCardOutput output = new CreditCardOutput("cc-1", "Nubank", java.util.List.of());

        CreditCardResponseDto dto = mapper.creditCardOutputToCreditCardResponseDto(output);

        assertThat(dto).isEqualTo(new CreditCardResponseDto("cc-1", "Nubank", java.util.List.of()));
    }

    @Test
    void toPagedResponse_mapsContentAndPagingFields() {
        var output1 = new CreditCardOutput("cc-1", "Nubank", java.util.List.of());
        var output2 = new CreditCardOutput("cc-2", "Itaú", java.util.List.of());
        PageResult<CreditCardOutput> page = new PageResult<>(List.of(output1, output2), 0, 20, 2, 1);

        PagedCreditCardResponseDto response = mapper.toPagedResponse(page);

        assertThat(response.content()).containsExactly(
                new CreditCardResponseDto("cc-1", "Nubank", java.util.List.of()),
                new CreditCardResponseDto("cc-2", "Itaú", java.util.List.of())
        );
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void toPagedResponse_emptyContent_returnsEmptyListWithPagingFields() {
        PageResult<CreditCardOutput> page = new PageResult<>(List.of(), 3, 10, 0, 0);

        PagedCreditCardResponseDto response = mapper.toPagedResponse(page);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }
}
