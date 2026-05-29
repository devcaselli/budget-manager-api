package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindAllSubscriptionsBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FindAllSubscriptionsUseCase implements FindAllSubscriptionsBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindAllSubscriptionsUseCase.class);

    private final SubscriptionRepository subscriptionRepository;

    public FindAllSubscriptionsUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public PageResult<SubscriptionOutput> execute(int page, int size, String ownerId) {
        log.debug("Finding all subscriptions page={}, size={}, ownerId={}", page, size, ownerId);

        PageResult<Subscription> subscriptionPage = subscriptionRepository.findAll(page, size, ownerId);
        List<SubscriptionOutput> outputs = subscriptionPage.content().stream()
                .map(SubscriptionOutputAssembler::from)
                .toList();

        return new PageResult<>(
                outputs,
                subscriptionPage.page(),
                subscriptionPage.size(),
                subscriptionPage.totalElements(),
                subscriptionPage.totalPages()
        );
    }
}
