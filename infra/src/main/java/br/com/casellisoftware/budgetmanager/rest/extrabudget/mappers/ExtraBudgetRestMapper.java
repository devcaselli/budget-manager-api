package br.com.casellisoftware.budgetmanager.rest.extrabudget.mappers;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.AllocationRequestDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.AllocationResponseDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetResponseDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records.
 */
@Mapper(config = ProjectMapper.class)
public interface ExtraBudgetRestMapper {

    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    ExtraBudgetInput toInput(ExtraBudgetRequestDto requestDto);

    AllocationInput toInput(AllocationRequestDto requestDto);

    @BeanMapping(ignoreUnmappedSourceProperties = {"ownerId"})
    ExtraBudgetResponseDto toResponse(ExtraBudgetOutput output);

    AllocationResponseDto toResponse(AllocationOutput output);
}
