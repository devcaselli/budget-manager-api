package br.com.casellisoftware.budgetmanager.rest.wallet.mappers;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletRestMapper {

    @Mapping(target = "isClosed", source = "closed")
    WalletInput walletRequestDtoToWalletInput(WalletRequestDto requestDto);

    @Mapping(target = "closed", source = "isClosed")
    WalletResponseDto walletOutputToWalletResponseDto(WalletOutput walletOutput);
}
