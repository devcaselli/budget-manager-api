package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSubscriptionDomainByIdUseCaseTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private FindSubscriptionDomainByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindSubscriptionDomainByIdUseCase(subscriptionRepository);
    }

    @Test
    void findById_whenFound_returnsDomainSubscription() {
        Subscription subscription = FindSubscriptionByIdUseCaseTest.subscription("subscription-1");
        when(subscriptionRepository.findById("subscription-1", "legacy")).thenReturn(Optional.of(subscription));

        Subscription result = useCase.findById("subscription-1", "legacy");

        assertThat(result).isSameAs(subscription);
    }

    @Test
    void findById_whenMissing_throws() {
        when(subscriptionRepository.findById("missing", "legacy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("missing", "legacy"))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
