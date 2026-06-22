package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductions;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.RepositoryBackedWalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class FindWalletByIdUseCase implements FindWalletByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindWalletByIdUseCase.class);

    private final WalletRepository walletRepository;
    private final WalletDeductionsQuery walletDeductionsQuery;

    public FindWalletByIdUseCase(WalletRepository walletRepository,
                                 WalletDeductionsQuery walletDeductionsQuery) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.walletDeductionsQuery = Objects.requireNonNull(walletDeductionsQuery, "walletDeductionsQuery must not be null");
    }

    public FindWalletByIdUseCase(WalletRepository walletRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 InstallmentRepository installmentRepository,
                                 ShareRepository shareRepository,
                                 ReservedBudgetRepository reservedBudgetRepository) {
        this(
                walletRepository,
                new RepositoryBackedWalletDeductionsQuery(
                        subscriptionRepository,
                        installmentRepository,
                        shareRepository,
                        reservedBudgetRepository)
        );
    }

    @Override
    public WalletOutput findById(String id, String ownerId) {
        log.debug("Finding wallet by id {} for owner {}", id, ownerId);
        Wallet wallet = walletRepository.findById(id, ownerId)
                .orElseThrow(() -> new WalletNotFoundException(id));

        log.debug("Wallet found {}", wallet);
        WalletDeductions deductions = walletDeductionsQuery.forWallet(wallet);
        return WalletOutputAssembler.from(wallet, deductions);
    }
}
