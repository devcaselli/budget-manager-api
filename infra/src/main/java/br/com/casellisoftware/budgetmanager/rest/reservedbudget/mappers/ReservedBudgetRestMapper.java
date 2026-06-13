package br.com.casellisoftware.budgetmanager.rest.reservedbudget.mappers;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.PagedReservedBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface ReservedBudgetRestMapper {

    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    ReservedBudgetInput toInput(ReservedBudgetRequestDto request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "details", source = "request.details")
    @Mapping(target = "newAmount", source = "request.newAmount")
    @Mapping(target = "flag", source = "request.flag")
    @Mapping(target = "effectiveMonth", source = "request.effectiveMonth")
    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    PatchReservedBudgetInput toPatchInput(String id, ReservedBudgetPatchRequestDto request);

    ReservedBudgetResponseDto toResponse(ReservedBudgetOutput output);

    default PagedReservedBudgetResponseDto toPagedResponse(PageResult<ReservedBudgetOutput> page) {
        List<ReservedBudgetResponseDto> content = page.content()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PagedReservedBudgetResponseDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    default PagedReservedBudgetResponseDto toPagedResponse(List<ReservedBudgetOutput> content) {
        List<ReservedBudgetResponseDto> responseContent = content.stream()
                .map(this::toResponse)
                .toList();
        int totalPages = responseContent.isEmpty() ? 0 : 1;

        return new PagedReservedBudgetResponseDto(
                responseContent,
                0,
                responseContent.size(),
                responseContent.size(),
                totalPages
        );
    }
}
