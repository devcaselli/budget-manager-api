package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionAlreadyEndedException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteSubscriptionUseCaseTest {

    private static final String OWNER = "legacy";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private FlagManager flagManager;

    private DeleteSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteSubscriptionUseCase(subscriptionRepository, shareRepository, FIXED_CLOCK, flagManager);
    }

    @Test
    void execute_softDeletesSubscriptionAtCurrentMonth() {
        Subscription existing = subscription(null);
        when(subscriptionRepository.findById("subscription-1", OWNER)).thenReturn(Optional.of(existing));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, "subscription-1", OWNER)).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("subscription-1", OWNER);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getEndMonth()).isEqualTo(YearMonth.of(2026, 7));
    }

    @Test
    void execute_whenSubscriptionDoesNotExist_throwsAndDoesNotSave() {
        when(subscriptionRepository.findById("missing", OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", OWNER))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining("missing");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenPreviewSubscription_hardDeletesAndDoesNotSaveEndMonth() {
        Subscription existing = subscription(null, SubscriptionState.PREVIEW);
        when(subscriptionRepository.findById("subscription-1", OWNER)).thenReturn(Optional.of(existing));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, "subscription-1", OWNER)).thenReturn(false);

        useCase.execute("subscription-1", OWNER);

        verify(subscriptionRepository).deleteById("subscription-1", OWNER);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenAlreadyEnded_propagatesAndDoesNotSave() {
        Subscription existing = subscription(YearMonth.of(2026, 6));
        when(subscriptionRepository.findById("subscription-1", OWNER)).thenReturn(Optional.of(existing));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, "subscription-1", OWNER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("subscription-1", OWNER))
                .isInstanceOf(SubscriptionAlreadyEndedException.class)
                .hasMessageContaining("subscription-1");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenFutureSubscriptionHasEnabledDateValidationFlag_hardDeletes() {
        Subscription existing = subscription(
                YearMonth.of(2026, 9),
                null,
                SubscriptionState.PRODUCTION,
                FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION
        );
        when(subscriptionRepository.findById("subscription-1", OWNER)).thenReturn(Optional.of(existing));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, "subscription-1", OWNER)).thenReturn(false);
        when(flagManager.isEnabled(FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION)).thenReturn(true);

        useCase.execute("subscription-1", OWNER);

        verify(subscriptionRepository).deleteById("subscription-1", OWNER);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenFutureSubscriptionFlagIsDisabled_propagatesDateValidationAndDoesNotDelete() {
        Subscription existing = subscription(
                YearMonth.of(2026, 9),
                null,
                SubscriptionState.PRODUCTION,
                FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION
        );
        when(subscriptionRepository.findById("subscription-1", OWNER)).thenReturn(Optional.of(existing));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, "subscription-1", OWNER)).thenReturn(false);
        when(flagManager.isEnabled(FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("subscription-1", OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endMonth must not be before startMonth");

        verify(subscriptionRepository, never()).deleteById(any());
        verify(subscriptionRepository, never()).save(any());
    }

    private static Subscription subscription(YearMonth endMonth) {
        return subscription(endMonth, SubscriptionState.PRODUCTION);
    }

    private static Subscription subscription(YearMonth endMonth, SubscriptionState state) {
        return subscription(YearMonth.of(2026, 5), endMonth, state, FlagEnum.NONE);
    }

    private static Subscription subscription(YearMonth startMonth,
                                             YearMonth endMonth,
                                             SubscriptionState state,
                                             FlagEnum flag) {
        return Subscription.rebuild(
                "subscription-1",
                "streaming",
                Money.DEFAULT_CURRENCY,
                startMonth,
                endMonth,
                state,
                List.of(new SubscriptionVersion(startMonth, Money.of("50.00"))),
                flag
        );
    }
}
