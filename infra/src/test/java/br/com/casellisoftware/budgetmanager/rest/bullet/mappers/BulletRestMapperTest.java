package br.com.casellisoftware.budgetmanager.rest.bullet.mappers;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BulletRestMapperTest {

    private final BulletRestMapper mapper = Mappers.getMapper(BulletRestMapper.class);

    @Test
    void bulletRequestDtoToBulletInput_copiesAllFields() {
        BulletRequestDto dto = new BulletRequestDto(
                "rent", new BigDecimal("1500.00"), "wallet-1"
        );

        BulletInput input = mapper.bulletRequestDtoToBulletInput(dto);

        assertThat(input)
                .usingRecursiveComparison()
                .isEqualTo(new BulletInput("rent", new BigDecimal("1500.00"), "wallet-1"));
    }

    @Test
    void bulletOutputToBulletResponseDto_copiesAllFields() {
        BulletOutput output = new BulletOutput(
                "id-1", "groceries", new BigDecimal("300.00"),
                new BigDecimal("150.00"), "wallet-2"
        );

        BulletResponseDto dto = mapper.bulletOutputToBulletResponseDto(output);

        assertThat(dto)
                .usingRecursiveComparison()
                .isEqualTo(new BulletResponseDto(
                        "id-1", "groceries", new BigDecimal("300.00"),
                        new BigDecimal("150.00"), "wallet-2"
                ));
    }
}
