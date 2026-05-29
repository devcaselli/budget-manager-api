package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.DeletePayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerInUseByShareException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;

import java.util.Objects;

public class DeletePayerByIdUseCase implements DeletePayerByIdBoundary {

    private final PayerRepository payerRepository;
    private final ShareRepository shareRepository;

    public DeletePayerByIdUseCase(PayerRepository payerRepository, ShareRepository shareRepository) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public void execute(String id, String ownerId) {
        if (!shareRepository.findActiveByPayerId(id, ownerId).isEmpty()) {
            throw new PayerInUseByShareException(id);
        }
        payerRepository.deleteById(id, ownerId);
    }
}
