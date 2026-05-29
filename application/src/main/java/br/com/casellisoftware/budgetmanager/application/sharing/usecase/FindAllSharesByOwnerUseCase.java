package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindAllSharesByOwnerBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;

import java.util.List;
import java.util.Objects;

public class FindAllSharesByOwnerUseCase implements FindAllSharesByOwnerBoundary {

    private final ShareRepository shareRepository;
    private final PayerRepository payerRepository;

    public FindAllSharesByOwnerUseCase(ShareRepository shareRepository, PayerRepository payerRepository) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
    }

    @Override
    public List<ShareOutput> execute(String ownerId) {
        return shareRepository.findAllByOwner(ownerId).stream()
                .map(share -> ShareOutputAssembler.from(share, payerRepository, ownerId))
                .toList();
    }
}
