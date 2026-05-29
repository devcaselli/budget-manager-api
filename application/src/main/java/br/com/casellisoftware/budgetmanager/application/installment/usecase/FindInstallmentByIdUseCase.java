package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.util.Objects;

public class FindInstallmentByIdUseCase implements FindInstallmentByIdBoundary {

    private final InstallmentRepository installmentRepository;
    private final ShareRepository shareRepository;

    public FindInstallmentByIdUseCase(InstallmentRepository installmentRepository,
                                      ShareRepository shareRepository) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public InstallmentOutput findById(String id, String ownerId) {
        Installment installment = installmentRepository.findById(id, ownerId)
                .orElseThrow(() -> new InstallmentNotFoundException(id));
        Share active = shareRepository
                .findActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), ownerId)
                .orElse(null);
        return InstallmentOutputAssembler.from(installment, active);
    }
}
