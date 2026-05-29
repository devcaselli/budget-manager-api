package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class PatchSubscriptionUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private PatchSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchSubscriptionUseCase(subscriptionRepository, FIXED_CLOCK);
    }

    @Test
    void execute_renamesAndAddsVersionAtCurrentMonthWhenAmountChanges() {
        Subscription existing = subscription();
        PatchSubscriptionInput input = new PatchSubscriptionInput(
                "subscription-1",
                "music",
                new BigDecimal("65.00")
        );
        when(subscriptionRepository.findById("subscription-1", "legacy")).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionOutput output = useCase.execute(input);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();

        assertThat(saved.getDescription()).isEqualTo("music");
        assertThat(saved.resolveAmount(YearMonth.of(2026, 6))).isEqualTo(Money.of("50.00"));
        assertThat(saved.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("65.00"));
        assertThat(saved.getVersions()).hasSize(2);
        assertThat(output.description()).isEqualTo("music");
        assertThat(output.versions()).hasSize(2);
    }

    @Test
    void execute_whenPatchDoesNotChangeState_savesExistingInstance() {
        Subscription existing = subscription();
        PatchSubscriptionInput input = new PatchSubscriptionInput(
                "subscription-1",
                "streaming",
                new BigDecimal("50.00")
        );
        when(subscriptionRepository.findById("subscription-1", "legacy")).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void execute_whenOnlyDescriptionProvided_preservesVersions() {
        Subscription existing = subscription();
        PatchSubscriptionInput input = new PatchSubscriptionInput("subscription-1", "music", null);
        when(subscriptionRepository.findById("subscription-1", "legacy")).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("music");
        assertThat(captor.getValue().getVersions()).hasSize(1);
    }

    @Test
    void execute_whenSubscriptionDoesNotExist_throwsAndDoesNotSave() {
        PatchSubscriptionInput input = new PatchSubscriptionInput("missing", "music", null);
        when(subscriptionRepository.findById("missing", "legacy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining("missing");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenInputIsNull_throwsAndDoesNotTouchRepository() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("input");

        verify(subscriptionRepository, never()).findById(any());
        verify(subscriptionRepository, never()).save(any());
    }

    private static Subscription subscription() {
        return Subscription.rebuild(
                "subscription-1",
                "streaming",
                Money.DEFAULT_CURRENCY,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(
                        YearMonth.of(2026, 5),
                        Money.of("50.00")
                )),
                FlagEnum.NONE
        );
    }
}
