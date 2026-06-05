package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindWalletSharesUseCaseTest {

    // Wallet's all-args ctor used here defaults ownerId to "legacy"; the selector
    // resolves the owner from the wallet, so the test owner must match it.
    private static final String OWNER = "legacy";
    private static final String SUB_ID = "sub-netflix";
    private static final Instant CREATED_AT = Instant.parse("2026-05-01T00:00:00Z");

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private ShareRepository shareRepository;
    @Mock
    private PayerRepository payerRepository;

    private FindWalletSharesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindWalletSharesUseCase(findWalletDomainByIdBoundary, subscriptionRepository,
                installmentRepository, shareRepository, payerRepository);
    }

    @Test
    void execute_subscriptionShareCreatedInPastWallet_surfacesInLaterWallet() {
        Wallet junWallet = wallet("wallet-jun", YearMonth.of(2026, 6));
        Share maioShare = subscriptionShare(null); // created in Maio, still active
        stubSubscriptionAndShare(junWallet, maioShare);

        List<ShareOutput> result = useCase.execute("wallet-jun", OWNER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).sourceId()).isEqualTo(SUB_ID);
        assertThat(result.get(0).quotas()).hasSize(1);
        assertThat(result.get(0).quotas().get(0).payerName()).isEqualTo("Mauro");
    }

    @Test
    void execute_shareStoppedFromThisMonth_isOmitted() {
        Wallet junWallet = wallet("wallet-jun", YearMonth.of(2026, 6));
        Share stopped = subscriptionShare(YearMonth.of(2026, 6));
        stubSubscriptionAndShare(junWallet, stopped);

        List<ShareOutput> result = useCase.execute("wallet-jun", OWNER);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_pastWalletStillSeesStoppedShare() {
        Wallet maioWallet = wallet("wallet-maio", YearMonth.of(2026, 5));
        Share stopped = subscriptionShare(YearMonth.of(2026, 6)); // stopped from Jun
        stubSubscriptionAndShare(maioWallet, stopped);

        List<ShareOutput> result = useCase.execute("wallet-maio", OWNER);

        assertThat(result).hasSize(1); // Maio (< Jun) keeps the share
    }

    @Test
    void execute_usesBatchShareQueries_noNPlusOne() {
        Wallet junWallet = wallet("wallet-jun", YearMonth.of(2026, 6));
        stubSubscriptionAndShare(junWallet, subscriptionShare(null));

        useCase.execute("wallet-jun", OWNER);

        verify(shareRepository, times(1))
                .findActiveBySourceIds(ShareSourceType.SUBSCRIPTION, List.of(SUB_ID), OWNER);
    }

    private void stubSubscriptionAndShare(Wallet wallet, Share share) {
        when(findWalletDomainByIdBoundary.findById(wallet.getId(), OWNER)).thenReturn(wallet);
        when(subscriptionRepository.findActiveFor(wallet.getEffectiveMonth(), SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(subscription()));
        when(installmentRepository.findActiveAffecting(wallet.getEffectiveMonth(), OWNER)).thenReturn(List.of());
        when(shareRepository.findActiveBySourceIds(ShareSourceType.SUBSCRIPTION, List.of(SUB_ID), OWNER))
                .thenReturn(Map.of(SUB_ID, share));
        if (share.isEffectiveFor(wallet.getEffectiveMonth())) {
            when(payerRepository.findAllByIdsIn(java.util.Set.of("payer-mauro"), OWNER))
                    .thenReturn(List.of(payer()));
        }
    }

    private static Wallet wallet(String id, YearMonth month) {
        return new Wallet(
                id,
                "wallet",
                Money.of("1000.00"),
                Money.of("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                month,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    private static Subscription subscription() {
        return Subscription.rebuild(
                SUB_ID,
                "Netflix",
                Currency.getInstance("BRL"),
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("100.00"))),
                FlagEnum.NONE
        );
    }

    private static Share subscriptionShare(YearMonth stoppedFromMonth) {
        Share share = Share.create(
                "wallet-maio",
                ShareSourceType.SUBSCRIPTION,
                SUB_ID,
                Money.of("100.00"),
                Money.of("50.00"),
                List.of(new Share.ShareQuotaAllocation("payer-mauro", Money.of("50.00"))),
                OWNER,
                CREATED_AT
        );
        return stoppedFromMonth == null ? share : share.stopFrom(stoppedFromMonth);
    }

    private static Payer payer() {
        return new Payer("payer-mauro", OWNER, "Mauro", PayerType.STANDING, null, null,
                LocalDate.of(2026, 5, 10), false);
    }
}
