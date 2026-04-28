package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.BulletAllocationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchBulletUseCase implements PatchBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public PatchBulletUseCase(BulletRepository bulletRepository,
                              WalletRepository walletRepository,
                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public BulletOutput execute(PatchBulletInput input) {
        log.info("Patching bullet id={}", input.id());

        Bullet existing = bulletRepository.findById(input.id())
                .orElseThrow(() -> new BulletNotFoundException(input.id()));

        Money requestedBudget = input.budget() == null ? existing.getBudget() : Money.of(input.budget());
        if (!existing.getBudget().equals(requestedBudget)) {
            reconcileWallet(existing, requestedBudget);
        }

        BulletPatch patch = toPatch(input, existing);
        if (log.isDebugEnabled()) {
            log.debug("Applying bullet patch id={}, fields={}", input.id(), patch.appliedFieldNames());
        }

        Bullet patched = existing.patch(patch);
        Bullet saved = bulletRepository.save(patched);
        log.info("Bullet patched successfully, id={}", saved.getId());

        return BulletOutputAssembler.from(saved);
    }

    private BulletPatch toPatch(PatchBulletInput input, Bullet existing) {
        Money requestedBudget = input.budget() == null ? null : Money.of(input.budget());
        Money requestedRemaining = input.remaining() == null ? null : Money.of(input.remaining());

        if (requestedBudget != null && !existing.getBudget().equals(requestedBudget)) {
            requestedRemaining = adjustedRemaining(existing, requestedBudget);
        }

        return BulletPatch.empty()
                .withDescription(input.description())
                .withBudget(requestedBudget)
                .withRemaining(requestedRemaining)
                .withWalletId(input.walletId());
    }

    private Money adjustedRemaining(Bullet existing, Money requestedBudget) {
        if (requestedBudget.isGreaterThan(existing.getBudget())) {
            return existing.getRemaining().add(requestedBudget.subtract(existing.getBudget()));
        }
        return existing.getRemaining().debitBy(existing.getBudget().subtract(requestedBudget));
    }

    private void reconcileWallet(Bullet existing, Money requestedBudget) {
        Wallet wallet = findWalletDomainByIdBoundary.findById(existing.getWalletId());

        BulletAllocationPolicy.validateReallocation(
                wallet,
                existing.getBudget(),
                requestedBudget,
                existing.getRemaining()
        );

        Wallet reconciled;
        if (requestedBudget.isGreaterThan(existing.getBudget())) {
            reconciled = wallet.debit(requestedBudget.subtract(existing.getBudget()));
        } else {
            reconciled = wallet.credit(existing.getBudget().subtract(requestedBudget));
        }

        walletRepository.save(reconciled);
    }
}
