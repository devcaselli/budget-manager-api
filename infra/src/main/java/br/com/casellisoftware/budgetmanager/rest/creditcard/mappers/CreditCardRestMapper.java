package br.com.casellisoftware.budgetmanager.rest.creditcard.mappers;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardChargesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardExpensesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardChargesResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardRequestDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardExpensesResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.PagedCreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface CreditCardRestMapper {

    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    CreditCardInput creditCardRequestDtoToCreditCardInput(CreditCardRequestDto dto);

    CreditCardResponseDto creditCardOutputToCreditCardResponseDto(CreditCardOutput output);

    @BeanMapping(ignoreUnmappedSourceProperties = {"paymentIds", "flag"})
    ExpenseResponseDto expenseOutputToExpenseResponseDto(ExpenseOutput output);

    InstallmentResponseDto installmentOutputToResponseDto(InstallmentOutput output);

    SubscriptionChargeResponseDto subscriptionChargeOutputToResponseDto(SubscriptionChargeOutput output);

    default PagedCreditCardResponseDto toPagedResponse(PageResult<CreditCardOutput> page) {
        List<CreditCardResponseDto> content = page.content().stream()
                .map(this::creditCardOutputToCreditCardResponseDto)
                .toList();
        return new PagedCreditCardResponseDto(
                content, page.page(), page.size(), page.totalElements(), page.totalPages());
    }

    default CreditCardExpensesResponseDto toCreditCardExpensesResponseDto(CreditCardExpensesOutput output) {
        List<ExpenseResponseDto> content = output.expenses().content().stream()
                .map(this::expenseOutputToExpenseResponseDto)
                .toList();

        return new CreditCardExpensesResponseDto(
                content,
                output.expenses().page(),
                output.expenses().size(),
                output.expenses().totalElements(),
                output.expenses().totalPages(),
                output.totalCost()
        );
    }

    default CreditCardChargesResponseDto toCreditCardChargesResponseDto(CreditCardChargesOutput output) {
        return new CreditCardChargesResponseDto(
                output.expenses().stream().map(this::expenseOutputToExpenseResponseDto).toList(),
                output.installments().stream().map(this::installmentOutputToResponseDto).toList(),
                output.subscriptions().stream().map(this::subscriptionChargeOutputToResponseDto).toList(),
                output.totalCost()
        );
    }
}
