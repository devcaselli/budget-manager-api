package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindSubscriptionByIdUseCase implements FindSubscriptionByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindSubscriptionByIdUseCase.class);

    private final SubscriptionRepository subscriptionRepository;

    public FindSubscriptionByIdUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public SubscriptionOutput execute(String id, String ownerId) {
        log.debug("Finding subscription by id={} ownerId={}", id, ownerId);

        Subscription subscription = subscriptionRepository.findById(id, ownerId)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));

        log.debug("Subscription found id={}", subscription.getId());
        return SubscriptionOutputAssembler.from(subscription);
    }
}
