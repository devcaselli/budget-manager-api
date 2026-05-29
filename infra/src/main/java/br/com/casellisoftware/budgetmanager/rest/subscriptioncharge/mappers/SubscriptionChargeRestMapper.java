package br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.mappers;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;
import org.mapstruct.Mapper;

@Mapper(config = ProjectMapper.class)
public interface SubscriptionChargeRestMapper {

    SubscriptionChargeResponseDto toResponse(SubscriptionChargeOutput output);
}
