package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductions;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.RepositoryBackedWalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FindAllWalletsUseCase implements FindAllWalletsBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindAllWalletsUseCase.class);

    private final WalletRepository walletRepository;
    private final WalletDeductionsQuery walletDeductionsQuery;

    public FindAllWalletsUseCase(WalletRepository walletRepository,
                                 WalletDeductionsQuery walletDeductionsQuery) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.walletDeductionsQuery = Objects.requireNonNull(walletDeductionsQuery, "walletDeductionsQuery must not be null");
    }

    public FindAllWalletsUseCase(WalletRepository walletRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 InstallmentRepository installmentRepository,
                                 ShareRepository shareRepository) {
        this(
                walletRepository,
                new RepositoryBackedWalletDeductionsQuery(
                        subscriptionRepository,
                        installmentRepository,
                        shareRepository)
        );
    }

    @Override
    public List<WalletOutput> execute(String ownerId) {
        log.debug("Finding all wallets for owner {}", ownerId);
        List<Wallet> wallets = walletRepository.findAll(ownerId);
        if (wallets.isEmpty()) {
            return List.of();
        }
        Map<String, WalletDeductions> deductionsByWalletId = walletDeductionsQuery.forWallets(wallets);
        return wallets.stream()
                .map(wallet -> WalletOutputAssembler.from(wallet, deductionsByWalletId.get(wallet.getId())))
                .toList();
    }
}
