package br.com.casellisoftware.budgetmanager.rest.bullet.mappers;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records.
 */
@Mapper(config = ProjectMapper.class)
public interface BulletRestMapper {

    BulletInput bulletRequestDtoToBulletInput(BulletRequestDto requestDto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", source = "requestDto.description")
    @Mapping(target = "budget", source = "requestDto.budget")
    @Mapping(target = "remaining", source = "requestDto.remaining")
    @Mapping(target = "walletId", source = "requestDto.walletId")
    PatchBulletInput bulletPatchRequestDtoToInput(String id, BulletPatchRequestDto requestDto);

    BulletResponseDto bulletOutputToBulletResponseDto(BulletOutput output);
}
