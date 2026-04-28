package br.com.casellisoftware.budgetmanager.rest.expense.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.PagedExpenseResponseDto;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records.
 *
 * <p>Policies:
 * <ul>
 *   <li>{@code unmappedTargetPolicy = ERROR} — adding a target field without a
 *       mapping fails the build.</li>
 *   <li>{@code unmappedSourcePolicy = ERROR} — adding a source field without
 *       consuming it fails the build.</li>
 * </ul>
 */
@Mapper(config = ProjectMapper.class)
public interface ExpenseRestMapper {

    ExpenseInput expenseRequestDtoToExpenseInput(ExpenseRequestDto expenseRequestDto);

    ExpenseResponseDto expenseOutputToExpenseResponseDto(ExpenseOutput expense);

    /**
     * MapStruct helper method to convert Money to BigDecimal.
     */
    default BigDecimal map(Money value) {
        return value == null ? null : value.amount();
    }

    /**
     * Converts a paged application result into the REST response DTO.
     * Hand-written because MapStruct cannot infer the generic {@code PageResult<ExpenseOutput>}
     * to {@code PagedExpenseResponseDto} mapping automatically.
     */
    default PagedExpenseResponseDto toPagedResponse(PageResult<ExpenseOutput> page) {
        List<ExpenseResponseDto> content = page.content().stream()
                .map(this::expenseOutputToExpenseResponseDto)
                .toList();

        return new PagedExpenseResponseDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }
}
