package br.com.casellisoftware.budgetmanager.rest.wallet.mappers;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletRestMapper {

    WalletInput walletRequestDtoToWalletInput(WalletRequestDto requestDto);
    WalletResponseDto walletOutputToWalletResponseDto(WalletOutput expenseOutput);

}
