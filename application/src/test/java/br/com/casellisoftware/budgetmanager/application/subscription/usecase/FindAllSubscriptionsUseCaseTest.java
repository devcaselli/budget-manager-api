package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllSubscriptionsUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private FindAllSubscriptionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindAllSubscriptionsUseCase(subscriptionRepository);
    }

    @Test
    void execute_mapsPageResult() {
        Subscription subscription = FindSubscriptionByIdUseCaseTest.subscription("subscription-1");
        when(subscriptionRepository.findAll(1, 10, "owner-1"))
                .thenReturn(new PageResult<>(List.of(subscription), 1, 10, 21, 3));

        PageResult<SubscriptionOutput> result = useCase.execute(1, 10, "owner-1");

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(21);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).extracting(SubscriptionOutput::id).containsExactly("subscription-1");
    }
}
