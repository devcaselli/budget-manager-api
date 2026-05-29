package br.com.casellisoftware.budgetmanager.rest.subscription.mappers;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.PagedSubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = ProjectMapper.class)
public interface SubscriptionRestMapper {

    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    SubscriptionInput toInput(SubscriptionRequestDto request);

    default SubscriptionState toSubscriptionState(String state) {
        return state == null ? null : SubscriptionState.valueOf(state.trim().toUpperCase(java.util.Locale.ROOT));
    }

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", source = "request.description")
    @Mapping(target = "newAmount", source = "request.newAmount")
    @Mapping(target = "creditCardId", source = "request.creditCardId")
    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    PatchSubscriptionInput toPatchInput(String id, SubscriptionPatchRequestDto request);

    SubscriptionResponseDto toResponse(SubscriptionOutput output);

    default PagedSubscriptionResponseDto toPagedResponse(PageResult<SubscriptionOutput> page) {
        List<SubscriptionResponseDto> content = page.content()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PagedSubscriptionResponseDto(
                content,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    default PagedSubscriptionResponseDto toPagedResponse(List<SubscriptionOutput> content) {
        List<SubscriptionResponseDto> responseContent = content.stream()
                .map(this::toResponse)
                .toList();
        int totalPages = responseContent.isEmpty() ? 0 : 1;

        return new PagedSubscriptionResponseDto(
                responseContent,
                0,
                responseContent.size(),
                responseContent.size(),
                totalPages
        );
    }
}
