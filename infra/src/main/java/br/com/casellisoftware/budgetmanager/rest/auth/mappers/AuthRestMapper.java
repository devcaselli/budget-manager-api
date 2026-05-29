package br.com.casellisoftware.budgetmanager.rest.auth.mappers;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.AuthInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RegisterUserInput;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.AuthRequestDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.RegisterRequestDto;
import br.com.casellisoftware.budgetmanager.rest.auth.dtos.TokenResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuthRestMapper {

    @Mapping(source = "password", target = "rawPassword")
    RegisterUserInput toRegisterInput(RegisterRequestDto dto);

    @Mapping(source = "password", target = "rawPassword")
    AuthInput toAuthInput(AuthRequestDto dto);

    TokenResponseDto toTokenResponse(TokenOutput output);
}
