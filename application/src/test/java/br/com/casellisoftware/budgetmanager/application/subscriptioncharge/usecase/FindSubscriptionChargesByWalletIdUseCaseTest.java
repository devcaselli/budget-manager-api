package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.usecase;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSubscriptionChargesByWalletIdUseCaseTest {

    private static final String WALLET_ID = "wallet-1";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    @Mock
    private br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository shareRepository;

    private FindSubscriptionChargesByWalletIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindSubscriptionChargesByWalletIdUseCase(subscriptionRepository, findWalletDomainByIdBoundary, shareRepository);
    }

    @Test
    void execute_happyPath_returnsMappedOutputs() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, "legacy")).thenReturn(wallet);
        Subscription first = subscription("subscription-1", "80.00");
        Subscription second = subscription("subscription-2", "20.00");
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of(first, second));

        List<SubscriptionChargeOutput> result = useCase.execute(WALLET_ID, "legacy");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).subscriptionId()).isEqualTo("subscription-1");
        assertThat(result.get(0).amount()).isEqualByComparingTo("80.00");
        assertThat(result.get(1).subscriptionId()).isEqualTo("subscription-2");
        assertThat(result.get(1).amount()).isEqualByComparingTo("20.00");
    }

    @Test
    void execute_whenWalletHasNoCharges_returnsEmptyList() {
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, "legacy")).thenReturn(wallet());
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of());

        List<SubscriptionChargeOutput> result = useCase.execute(WALLET_ID, "legacy");

        assertThat(result).isEmpty();
    }

    @Test
    void execute_whenWalletDoesNotExist_propagatesAndDoesNotQueryCharges() {
        when(findWalletDomainByIdBoundary.findById("missing", "legacy")).thenThrow(new WalletNotFoundException("missing"));

        assertThatThrownBy(() -> useCase.execute("missing", "legacy"))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void execute_whenWalletIsPreview_returnsProductionAndPreviewSubscriptionCharges() {
        Wallet previewWallet = previewWallet();
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, "legacy")).thenReturn(previewWallet);
        Subscription production = subscription("subscription-prod", "80.00", SubscriptionState.PRODUCTION);
        Subscription preview = subscription("subscription-preview", "20.00", SubscriptionState.PREVIEW);
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PREVIEW, "legacy"))
                .thenReturn(List.of(production, preview));

        List<SubscriptionChargeOutput> result = useCase.execute(WALLET_ID, "legacy");

        assertThat(result).hasSize(2);
        assertThat(result)
                .anySatisfy(charge -> {
                    assertThat(charge.subscriptionId()).isEqualTo("subscription-prod");
                    assertThat(charge.amount()).isEqualByComparingTo("80.00");
                })
                .anySatisfy(charge -> {
                    assertThat(charge.subscriptionId()).isEqualTo("subscription-preview");
                    assertThat(charge.amount()).isEqualByComparingTo("20.00");
                });
    }

    @Test
    void execute_whenSubscriptionHasActiveShare_enrichesOutputWithEffectiveOwnerAmount() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, "legacy")).thenReturn(wallet);
        Subscription sub = subscription("subscription-1", "50.00");
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of(sub));

        br.com.casellisoftware.budgetmanager.domain.sharing.Share share = new br.com.casellisoftware.budgetmanager.domain.sharing.Share(
                "share-1",
                "owner-1",
                WALLET_ID,
                br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.SUBSCRIPTION,
                "subscription-1",
                Money.of("50.00"),
                Money.of("10.00"),
                new java.math.BigDecimal("0.20000000"),
                List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota(
                        "payer-1", new java.math.BigDecimal("0.80000000"), List.of())),
                br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus.ACTIVE,
                List.of(),
                java.time.Instant.parse("2026-05-01T00:00:00Z"),
                null,
                null
        );
        when(shareRepository.findActiveBySourceId(
                br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.SUBSCRIPTION,
                "subscription-1",
                sub.getOwnerId()
        )).thenReturn(java.util.Optional.of(share));

        List<SubscriptionChargeOutput> result = useCase.execute(WALLET_ID, "legacy");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).shared()).isTrue();
        assertThat(result.get(0).effectiveOwnerAmount()).isEqualByComparingTo("10.00");
        assertThat(result.get(0).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void execute_whenWalletIsProduction_returnsOnlyProductionSubscriptionCharges() {
        Wallet productionWallet = wallet();
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, "legacy")).thenReturn(productionWallet);
        Subscription production = subscription("subscription-prod", "100.00", SubscriptionState.PRODUCTION);
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(production));

        List<SubscriptionChargeOutput> result = useCase.execute(WALLET_ID, "legacy");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).subscriptionId()).isEqualTo("subscription-prod");
        assertThat(result.get(0).amount()).isEqualByComparingTo("100.00");
    }

    private static Wallet wallet() {
        return new Wallet(
                WALLET_ID,
                "wallet",
                Money.of("1000.00"),
                Money.of("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                YearMonth.of(2026, 5),
                br.com.casellisoftware.budgetmanager.domain.wallet.WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    private static Wallet previewWallet() {
        return new Wallet(
                WALLET_ID,
                "preview-wallet",
                Money.of("1000.00"),
                Money.of("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                YearMonth.of(2026, 5),
                br.com.casellisoftware.budgetmanager.domain.wallet.WalletState.PREVIEW,
                FlagEnum.NONE
        );
    }

    private static Subscription subscription(String id, String amount, SubscriptionState state) {
        return Subscription.rebuild(
                id,
                "subscription",
                Currency.getInstance("BRL"),
                YearMonth.of(2026, 5),
                null,
                state,
                List.of(new br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion(
                        YearMonth.of(2026, 5),
                        Money.of(amount)
                )),
                FlagEnum.NONE
        );
    }

    private static Subscription subscription(String id, String amount) {
        return subscription(id, amount, SubscriptionState.PRODUCTION);
    }
}
