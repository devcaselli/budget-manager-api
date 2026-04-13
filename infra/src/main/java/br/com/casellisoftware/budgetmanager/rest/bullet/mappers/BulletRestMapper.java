package br.com.casellisoftware.budgetmanager.rest.bullet.mappers;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Strict MapStruct mapper for REST DTO ↔ application boundary records.
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface BulletRestMapper {

    BulletInput bulletRequestDtoToBulletInput(BulletRequestDto requestDto);

    BulletResponseDto bulletOutputToBulletResponseDto(BulletOutput output);
}
