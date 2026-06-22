package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductions;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.RepositoryBackedWalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInputAssembler;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletPatch;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.WalletUniquenessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Objects;

public class PatchWalletUseCase implements PatchWalletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchWalletUseCase.class);

    private final WalletRepository walletRepository;
    private final WalletDeductionsQuery walletDeductionsQuery;
    private final Clock clock;

    public PatchWalletUseCase(WalletRepository walletRepository,
                              WalletDeductionsQuery walletDeductionsQuery,
                              Clock clock) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.walletDeductionsQuery = Objects.requireNonNull(walletDeductionsQuery, "walletDeductionsQuery must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PatchWalletUseCase(WalletRepository walletRepository,
                              SubscriptionRepository subscriptionRepository,
                              InstallmentRepository installmentRepository,
                              ShareRepository shareRepository,
                              ReservedBudgetRepository reservedBudgetRepository,
                              Clock clock) {
        this(
                walletRepository,
                new RepositoryBackedWalletDeductionsQuery(
                        subscriptionRepository,
                        installmentRepository,
                        shareRepository,
                        reservedBudgetRepository),
                clock
        );
    }

    @Override
    public WalletOutput execute(PatchWalletInput input) {
        log.info("Patching wallet id={}", input.id());

        Wallet existing = walletRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new WalletNotFoundException(input.id()));

        WalletPatch patch = PatchWalletInputAssembler.toPatch(input);
        if (log.isDebugEnabled()) {
            log.debug("Applying wallet patch id={}, fields={}", input.id(), patch.appliedFieldNames());
        }
        Wallet patched = existing.patch(patch);

        // If transitioning to PRODUCTION (e.g. PREVIEW->PRODUCTION) enforce
        // open-month uniqueness before persisting.
        WalletUniquenessPolicy.validate(walletRepository, patched, clock);

        Wallet saved = walletRepository.save(patched);
        log.info("Wallet patched successfully, id={}", saved.getId());

        WalletDeductions deductions = walletDeductionsQuery.forWallet(saved);
        return WalletOutputAssembler.from(saved, deductions);
    }
}
