package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveSubscriptionUseCaseTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-15T10:00:00Z"),
            ZoneOffset.UTC
    );
    private static final String CC_ID = "cc-1";

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CreditCardRepository creditCardRepository;

    private SaveSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveSubscriptionUseCase(subscriptionRepository, creditCardRepository, FIXED_CLOCK);
        lenient().when(creditCardRepository.existsById(anyString(), anyString())).thenReturn(true);
    }

    private SubscriptionInput buildInput(String description, BigDecimal amount, String currency) {
        return new SubscriptionInput(description, amount, currency, null, null, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID, CC_ID);
    }

    private SubscriptionInput buildInput(String description, BigDecimal amount, String currency,
                                         YearMonth effectiveMonth, SubscriptionState state) {
        return new SubscriptionInput(description, amount, currency, effectiveMonth, state, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID, CC_ID);
    }

    @Test
    void execute_createsSubscriptionForCurrentMonthAndReturnsOutput() {
        SubscriptionInput input = buildInput("streaming", new BigDecimal("55.90"), "BRL");
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionOutput output = useCase.execute(input);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getDescription()).isEqualTo("streaming");
        assertThat(saved.getCurrency().getCurrencyCode()).isEqualTo("BRL");
        assertThat(saved.getStartMonth()).isEqualTo(YearMonth.of(2026, 7));
        assertThat(saved.getEndMonth()).isNull();
        assertThat(saved.getCreditCardId()).isEqualTo(CC_ID);
        assertThat(saved.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("55.90"));

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.description()).isEqualTo("streaming");
        assertThat(output.currency()).isEqualTo("BRL");
        assertThat(output.startMonth()).isEqualTo(YearMonth.of(2026, 7));
        assertThat(output.versions()).hasSize(1);
        assertThat(output.creditCardId()).isEqualTo(CC_ID);
    }

    @Test
    void execute_withFutureEffectiveMonthAndPreviewState_usesInputValues() {
        SubscriptionInput input = buildInput(
                "future streaming",
                new BigDecimal("80.00"),
                "BRL",
                YearMonth.of(2026, 9),
                SubscriptionState.PREVIEW);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionOutput output = useCase.execute(input);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();

        assertThat(saved.getStartMonth()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(saved.getState()).isEqualTo(SubscriptionState.PREVIEW);
        assertThat(saved.isApplicable(YearMonth.of(2026, 8))).isFalse();
        assertThat(saved.isApplicable(YearMonth.of(2026, 9))).isTrue();
        assertThat(output.startMonth()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(output.state()).isEqualTo("PREVIEW");
    }

    @Test
    void execute_whenInputIsNull_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("input");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenCurrencyIsNull_throwsAndDoesNotSave() {
        SubscriptionInput input = buildInput("streaming", new BigDecimal("55.90"), null);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenCreditCardIdIsNull_throwsAndDoesNotSave() {
        SubscriptionInput input = new SubscriptionInput(
                "streaming", new BigDecimal("55.90"), "BRL", null, null, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID, null);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("creditCardId");

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenCreditCardDoesNotExist_throwsAndDoesNotSave() {
        SubscriptionInput input = buildInput("streaming", new BigDecimal("55.90"), "BRL");
        when(creditCardRepository.existsById(CC_ID, AuthenticatedUser.LEGACY_OWNER_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(CreditCardNotFoundException.class);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void execute_whenRepositoryFails_propagates() {
        SubscriptionInput input = buildInput("streaming", new BigDecimal("55.90"), "BRL");
        RuntimeException boom = new RuntimeException("mongo down");
        when(subscriptionRepository.save(any(Subscription.class))).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);
    }
}
