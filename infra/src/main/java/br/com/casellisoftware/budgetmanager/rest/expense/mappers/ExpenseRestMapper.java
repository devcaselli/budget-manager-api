package br.com.casellisoftware.budgetmanager.rest.expense.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records.
 *
 * <p>Policies:
 * <ul>
 *   <li>{@code unmappedTargetPolicy = ERROR} — adding a target field without a
 *       mapping fails the build.</li>
 *   <li>{@code unmappedSourcePolicy = ERROR} — adding a source field without
 *       consuming it fails the build.</li>
 *   <li>{@code nullValueCheckStrategy = ALWAYS} — defend against nulls at
 *       every mapping step.</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface ExpenseRestMapper {

    ExpenseInput expenseRequestDtoToExpenseInput(ExpenseRequestDto expenseRequestDto);

    ExpenseResponseDto expenseOutputToExpenseResponseDto(ExpenseOutput expense);
}
