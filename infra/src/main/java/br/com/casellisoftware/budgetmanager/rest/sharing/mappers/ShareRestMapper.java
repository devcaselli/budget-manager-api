package br.com.casellisoftware.budgetmanager.rest.sharing.mappers;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareQuotaInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareQuotaOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.TransientPayerSpec;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareQuotaRequestDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareQuotaResponseDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareRequestDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShareRestMapper {

    public ShareInput toInput(ShareRequestDto dto) {
        List<ShareQuotaInput> quotas = dto.quotas().stream()
                .map(this::toQuotaInput)
                .toList();
        return new ShareInput(
                dto.walletId(),
                dto.sourceType(),
                dto.sourceId(),
                dto.totalAmount(),
                dto.currency(),
                dto.ownerShare(),
                quotas
        );
    }

    public ShareResponseDto toResponse(ShareOutput output) {
        return new ShareResponseDto(
                output.id(),
                output.walletId(),
                output.sourceType(),
                output.sourceId(),
                output.totalAmount(),
                output.ownerShare(),
                output.ownerRatio(),
                output.currency(),
                output.status(),
                output.quotas().stream().map(this::toQuotaResponse).toList(),
                output.paymentIds(),
                output.createdAt(),
                output.revertedAt()
        );
    }

    private ShareQuotaInput toQuotaInput(ShareQuotaRequestDto dto) {
        TransientPayerSpec transientSpec = dto.transient_() == null
                ? null
                : new TransientPayerSpec(dto.transient_().name(), dto.transient_().paymentDate());
        return new ShareQuotaInput(dto.payerId(), transientSpec, dto.amount());
    }

    private ShareQuotaResponseDto toQuotaResponse(ShareQuotaOutput output) {
        return new ShareQuotaResponseDto(
                output.payerId(),
                output.payerName(),
                output.ratio(),
                output.amount(),
                output.paymentIds()
        );
    }
}
