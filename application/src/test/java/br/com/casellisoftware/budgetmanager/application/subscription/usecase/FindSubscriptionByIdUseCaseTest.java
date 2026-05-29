package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSubscriptionByIdUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private FindSubscriptionByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindSubscriptionByIdUseCase(subscriptionRepository);
    }

    @Test
    void execute_whenFound_returnsOutput() {
        when(subscriptionRepository.findById("subscription-1", "legacy")).thenReturn(Optional.of(subscription("subscription-1")));

        SubscriptionOutput output = useCase.execute("subscription-1", "legacy");

        assertThat(output.id()).isEqualTo("subscription-1");
        assertThat(output.description()).isEqualTo("streaming");
        assertThat(output.currency()).isEqualTo("BRL");
        assertThat(output.versions()).hasSize(1);
    }

    @Test
    void execute_whenMissing_throws() {
        when(subscriptionRepository.findById("missing", "legacy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", "legacy"))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining("missing");
    }

    static Subscription subscription(String id) {
        return Subscription.rebuild(
                id,
                "streaming",
                Money.DEFAULT_CURRENCY,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00"))),
                FlagEnum.NONE
        );
    }
}
