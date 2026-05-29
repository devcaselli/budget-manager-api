package br.com.casellisoftware.budgetmanager.rest.installment.mappers;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentInput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.PagedInstallmentResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface InstallmentRestMapper {

    InstallmentResponseDto installmentOutputToInstallmentResponseDto(InstallmentOutput output);

    /**
     * Converts a paged application result into the REST response DTO.
     * Hand-written because MapStruct cannot infer the generic {@code PageResult<InstallmentOutput>}
     * to {@code PagedInstallmentResponseDto} mapping automatically.
     */
    default PagedInstallmentResponseDto toPagedResponse(PageResult<InstallmentOutput> page) {
        List<InstallmentResponseDto> content = page.content().stream()
                .map(this::installmentOutputToInstallmentResponseDto)
                .toList();

        return new PagedInstallmentResponseDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    default PatchInstallmentInput toPatchInput(String id, InstallmentPatchRequestDto request) {
        return new PatchInstallmentInput(
                id,
                request.details(),
                request.flag(),
                request.originalValue(),
                request.installmentValue(),
                request.installmentNumber(),
                request.sourceEffectiveMonth(),
                request.purchaseDate(),
                request.creditCardId(),
                null  // ownerId set by controller via withOwnerId()
        );
    }
}
