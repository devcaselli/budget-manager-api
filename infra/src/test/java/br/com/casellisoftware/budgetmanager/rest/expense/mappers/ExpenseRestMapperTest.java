package br.com.casellisoftware.budgetmanager.rest.expense.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

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
}
