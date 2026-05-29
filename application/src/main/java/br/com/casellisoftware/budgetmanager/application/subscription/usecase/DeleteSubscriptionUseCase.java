package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.SourceInUseByShareException;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.EndMonthBeforeStartMonthException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;

public class DeleteSubscriptionUseCase implements DeleteSubscriptionBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteSubscriptionUseCase.class);

    private final SubscriptionRepository subscriptionRepository;
    private final ShareRepository shareRepository;
    private final Clock clock;
    private final FlagManager flagManager;

    public DeleteSubscriptionUseCase(SubscriptionRepository subscriptionRepository,
                                     ShareRepository shareRepository,
                                     Clock clock) {
        this(subscriptionRepository, shareRepository, clock, ignored -> false);
    }

    public DeleteSubscriptionUseCase(SubscriptionRepository subscriptionRepository,
                                     ShareRepository shareRepository,
                                     Clock clock,
                                     FlagManager flagManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.shareRepository = shareRepository;
        this.clock = clock;
        this.flagManager = flagManager;
    }

    @Override
    public void execute(String id, String ownerId) {
        log.info("Deleting subscription id={} ownerId={}", id, ownerId);

        Subscription existing = subscriptionRepository.findById(id, ownerId)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));

        if (shareRepository.existsActiveBySourceId(ShareSourceType.SUBSCRIPTION, id, ownerId)) {
            throw new SourceInUseByShareException(ShareSourceType.SUBSCRIPTION, id);
        }

        if (existing.getState().isPreview()) {
            subscriptionRepository.deleteById(existing.getId(), ownerId);
            log.info("Subscription hard-deleted id={} state={}", existing.getId(), existing.getState());
            return;
        }

        try {
            Subscription ended = existing.endAt(YearMonth.now(clock));
            subscriptionRepository.save(ended);
            log.info("Subscription soft-deleted id={} endMonth={}", ended.getId(), ended.getEndMonth());
        } catch (EndMonthBeforeStartMonthException ex) {
            if (!canIgnoreDateValidation(existing)) {
                throw ex;
            }
            subscriptionRepository.deleteById(existing.getId(), ownerId);
            log.info("Subscription hard-deleted id={} flag={}", existing.getId(), existing.getFlag());
        }
    }

    private boolean canIgnoreDateValidation(Subscription subscription) {
        return subscription.getFlag() == FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION
                && flagManager.isEnabled(FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION);
    }
}
