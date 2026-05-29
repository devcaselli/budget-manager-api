package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindActiveSubscriptionsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class FindActiveSubscriptionsByMonthUseCase implements FindActiveSubscriptionsByMonthBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindActiveSubscriptionsByMonthUseCase.class);

    private final SubscriptionRepository subscriptionRepository;

    public FindActiveSubscriptionsByMonthUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public List<SubscriptionOutput> execute(YearMonth month, String ownerId) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        log.debug("Finding active subscriptions for month={} ownerId={}", targetMonth, ownerId);

        return subscriptionRepository.findActiveForByOwnerId(targetMonth, ownerId)
                .stream()
                .map(SubscriptionOutputAssembler::from)
                .toList();
    }
}
