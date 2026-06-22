package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductions;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.RepositoryBackedWalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.WalletUniquenessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

public class SaveWalletUseCase implements SaveWalletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveWalletUseCase.class);

    private final WalletRepository walletRepository;
    private final WalletDeductionsQuery walletDeductionsQuery;
    private final Clock clock;

    public SaveWalletUseCase(WalletRepository walletRepository,
                             WalletDeductionsQuery walletDeductionsQuery,
                             Clock clock) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.walletDeductionsQuery = Objects.requireNonNull(walletDeductionsQuery, "walletDeductionsQuery must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SaveWalletUseCase(WalletRepository walletRepository,
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
    public WalletOutput execute(WalletInput input) {
        YearMonth effectiveMonth = input.effectiveMonth() != null
                ? input.effectiveMonth()
                : (input.startDate() != null ? YearMonth.from(input.startDate()) : null);
        WalletState state = input.state() != null ? input.state() : WalletState.PRODUCTION;

        Wallet wallet = Wallet.create(
                input.description(),
                Money.of(input.budget()),
                input.closedDate(),
                input.startDate(),
                input.isClosed(),
                effectiveMonth,
                state,
                input.flag(),
                input.ownerId()
        );

        // PRODUCTION uniqueness check up-front (DB unique index is the safety net).
        WalletUniquenessPolicy.validate(walletRepository, wallet, clock);

        WalletDeductions deductions = walletDeductionsQuery.forWallet(wallet);
        if (deductions.subscriptions().isGreaterThan(wallet.getRemaining())) {
            throw new WalletAllocationExceededException(
                    "Subscription total " + deductions.subscriptions().amount()
                            + " exceeds wallet remaining " + wallet.getRemaining().amount()
                            + " (walletId=" + wallet.getId() + ")");
        }

        Wallet savedWallet = this.walletRepository.save(wallet);
        WalletOutput saved = WalletOutputAssembler.from(savedWallet, walletDeductionsQuery.forWallet(savedWallet));

        log.info("Wallet saved id={} effectiveMonth={} state={}",
                saved.id(), saved.effectiveMonth(), saved.state());

        return saved;
    }
}
