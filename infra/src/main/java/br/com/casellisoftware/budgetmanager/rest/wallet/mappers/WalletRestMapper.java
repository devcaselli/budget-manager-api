package br.com.casellisoftware.budgetmanager.rest.wallet.mappers;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ProjectMapper.class)
public interface WalletRestMapper {

    @Mapping(target = "isClosed", source = "closed")
    @Mapping(target = "effectiveMonth", source = "effectiveMonth")
    @Mapping(target = "state", source = "state")
    @Mapping(target = "ownerId", constant = "legacy")
    @Mapping(target = "withOwnerId", ignore = true)
    WalletInput walletRequestDtoToWalletInput(WalletRequestDto requestDto);

    @Mapping(target = "closed", source = "isClosed")
    @Mapping(target = "effectiveMonth", source = "effectiveMonth")
    @Mapping(target = "state", source = "state")
    WalletResponseDto walletOutputToWalletResponseDto(WalletOutput walletOutput);
}
