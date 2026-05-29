package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindActiveShareBySourceBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.util.Objects;

public class FindActiveShareBySourceUseCase implements FindActiveShareBySourceBoundary {

    private final ShareRepository shareRepository;
    private final PayerRepository payerRepository;

    public FindActiveShareBySourceUseCase(ShareRepository shareRepository, PayerRepository payerRepository) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
    }

    @Override
    public ShareOutput execute(ShareSourceType sourceType, String sourceId, String ownerId) {
        Share share = shareRepository.findActiveBySourceId(sourceType, sourceId, ownerId)
                .orElseThrow(() -> new ShareNotFoundException(sourceType + ":" + sourceId));
        return ShareOutputAssembler.from(share, payerRepository, ownerId);
    }
}
