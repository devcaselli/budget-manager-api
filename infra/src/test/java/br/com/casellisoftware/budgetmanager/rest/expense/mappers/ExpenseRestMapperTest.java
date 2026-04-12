package br.com.casellisoftware.budgetmanager.rest.expense.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.PagedExpenseResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseRestMapperTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(1);

    private final ExpenseRestMapper mapper = Mappers.getMapper(ExpenseRestMapper.class);

    @Test
    void expenseRequestDtoToExpenseInput_copiesAllFields() {
        ExpenseRequestDto dto = new ExpenseRequestDto(
                "lunch",
                new BigDecimal("10.50"),
                PURCHASE_DATE,
                "wallet-1"
        );

        ExpenseInput input = mapper.expenseRequestDtoToExpenseInput(dto);

        assertThat(input)
                .usingRecursiveComparison()
                .isEqualTo(new ExpenseInput("lunch", new BigDecimal("10.50"), PURCHASE_DATE, "wallet-1"));
    }

    @Test
    void expenseOutputToExpenseResponseDto_copiesAllFields() {
        ExpenseOutput output = new ExpenseOutput(
                "id-1",
                "coffee",
                new BigDecimal("5.00"),
                PURCHASE_DATE,
                "wallet-2",
                new BigDecimal("2.50")
        );

        ExpenseResponseDto dto = mapper.expenseOutputToExpenseResponseDto(output);

        assertThat(dto)
                .usingRecursiveComparison()
                .isEqualTo(new ExpenseResponseDto(
                        "id-1",
                        "coffee",
                        new BigDecimal("5.00"),
                        PURCHASE_DATE,
                        new BigDecimal("2.50"),
                        "wallet-2"
                ));
    }

    @Test
    void toPagedResponse_mapsAllFieldsAndContent() {
        ExpenseOutput output1 = new ExpenseOutput(
                "id-1", "lunch", new BigDecimal("10.50"), PURCHASE_DATE, "wallet-1", new BigDecimal("10.50"));
        ExpenseOutput output2 = new ExpenseOutput(
                "id-2", "coffee", new BigDecimal("5.00"), PURCHASE_DATE, "wallet-1", new BigDecimal("5.00"));

        PageResult<ExpenseOutput> page = new PageResult<>(
                List.of(output1, output2), 0, 10, 2, 1
        );

        PagedExpenseResponseDto dto = mapper.toPagedResponse(page);

        assertThat(dto.page()).isZero();
        assertThat(dto.size()).isEqualTo(10);
        assertThat(dto.totalElements()).isEqualTo(2);
        assertThat(dto.totalPages()).isEqualTo(1);
        assertThat(dto.content()).hasSize(2);
        assertThat(dto.content().get(0).id()).isEqualTo("id-1");
        assertThat(dto.content().get(0).name()).isEqualTo("lunch");
        assertThat(dto.content().get(1).id()).isEqualTo("id-2");
        assertThat(dto.content().get(1).name()).isEqualTo("coffee");
    }

    @Test
    void toPagedResponse_emptyPage_returnsEmptyContent() {
        PageResult<ExpenseOutput> page = new PageResult<>(List.of(), 0, 10, 0, 0);

        PagedExpenseResponseDto dto = mapper.toPagedResponse(page);

        assertThat(dto.content()).isEmpty();
        assertThat(dto.totalElements()).isZero();
        assertThat(dto.totalPages()).isZero();
    }
}
