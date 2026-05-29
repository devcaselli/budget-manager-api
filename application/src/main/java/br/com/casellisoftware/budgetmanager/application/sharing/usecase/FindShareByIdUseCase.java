package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindShareByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;

import java.util.Objects;

public class FindShareByIdUseCase implements FindShareByIdBoundary {

    private final ShareRepository shareRepository;
    private final PayerRepository payerRepository;

    public FindShareByIdUseCase(ShareRepository shareRepository, PayerRepository payerRepository) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
    }

    @Override
    public ShareOutput execute(String shareId, String ownerId) {
        Share share = shareRepository.findById(shareId, ownerId)
                .orElseThrow(() -> new ShareNotFoundException(shareId));
        return ShareOutputAssembler.from(share, payerRepository, ownerId);
    }
}
