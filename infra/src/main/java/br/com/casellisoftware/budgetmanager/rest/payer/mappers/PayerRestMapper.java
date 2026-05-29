package br.com.casellisoftware.budgetmanager.rest.payer.mappers;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import br.com.casellisoftware.budgetmanager.configs.mapstruct.ProjectMapper;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerResponseDto;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(config = ProjectMapper.class)
public interface PayerRestMapper {

    default PayerInput payerRequestDtoToPayerInput(PayerRequestDto dto) {
        return new PayerInput(
                dto.name(),
                dto.type(),
                dto.walletId(),
                dto.subscriptionId(),
                dto.paymentDate(),
                Payer.LEGACY_OWNER_ID
        );
    }

    default PayerPatchInput payerPatchRequestDtoToPayerPatchInput(PayerPatchRequestDto dto) {
        return new PayerPatchInput(
                Optional.ofNullable(dto.name()),
                Optional.ofNullable(dto.type()),
                Optional.ofNullable(dto.walletId()),
                Optional.ofNullable(dto.subscriptionId()),
                Optional.ofNullable(dto.paymentDate())
        );
    }

    default PayerResponseDto payerOutputToPayerResponseDto(PayerOutput output) {
        return new PayerResponseDto(
                output.id(),
                output.name(),
                output.type(),
                output.walletId(),
                output.subscriptionId(),
                output.paymentDate(),
                output.amountDue().amount(),
                output.monthlyAmount().amount(),
                output.journeyAmount().amount(),
                output.amountDue().currency().getCurrencyCode(),
                output.deleted()
        );
    }
}
