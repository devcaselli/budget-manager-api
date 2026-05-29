package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentWalletFilter;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.util.List;
import java.util.Objects;

public class FindInstallmentsByWalletIdUseCase implements FindInstallmentsByWalletIdBoundary {

    private final InstallmentRepository installmentRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    private final ShareRepository shareRepository;

    public FindInstallmentsByWalletIdUseCase(InstallmentRepository installmentRepository,
                                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                             ShareRepository shareRepository) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary, "findWalletDomainByIdBoundary must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public PageResult<InstallmentOutput> execute(String walletId, InstallmentWalletFilter filter, String ownerId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(filter, "filter must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Wallet wallet = findWalletOrThrow(walletId, ownerId);

        return installmentRepository.findByWalletContext(
                walletId,
                wallet.getEffectiveMonth(),
                filter.creditCardId(),
                filter.sortOrder(),
                filter.page(),
                filter.size(),
                ownerId
        ).map(installment -> enrichWithShare(installment, ownerId));
    }

    @Override
    public List<InstallmentOutput> executeAll(String walletId, String creditCardId, InstallmentSortOrder sortOrder, String ownerId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Wallet wallet = findWalletOrThrow(walletId, ownerId);
        InstallmentSortOrder effectiveSortOrder = sortOrder == null ? InstallmentSortOrder.ENDING_SOON : sortOrder;

        return installmentRepository.findByWalletContext(
                        walletId,
                        wallet.getEffectiveMonth(),
                        creditCardId,
                        effectiveSortOrder,
                        ownerId
                ).stream()
                .map(installment -> enrichWithShare(installment, ownerId))
                .toList();
    }

    private Wallet findWalletOrThrow(String walletId, String ownerId) {
        return findWalletDomainByIdBoundary.findById(walletId, ownerId);
    }

    private InstallmentOutput enrichWithShare(Installment installment, String ownerId) {
        Share active = shareRepository
                .findActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), ownerId)
                .orElse(null);
        return InstallmentOutputAssembler.from(installment, active);
    }
}
