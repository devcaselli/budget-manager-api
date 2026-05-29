package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindActiveSubscriptionsByMonthUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private FindActiveSubscriptionsByMonthUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindActiveSubscriptionsByMonthUseCase(subscriptionRepository);
    }

    private static final String OWNER = "owner-1";

    @Test
    void execute_delegatesMonthAndMapsSubscriptions() {
        YearMonth month = YearMonth.of(2026, 7);
        Subscription subscription = FindSubscriptionByIdUseCaseTest.subscription("subscription-1");
        when(subscriptionRepository.findActiveForByOwnerId(month, OWNER)).thenReturn(List.of(subscription));

        List<SubscriptionOutput> outputs = useCase.execute(month, OWNER);

        assertThat(outputs).extracting(SubscriptionOutput::id).containsExactly("subscription-1");
    }

    @Test
    void execute_whenMonthIsNull_throws() {
        assertThatThrownBy(() -> useCase.execute(null, OWNER))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("month");
    }
}
