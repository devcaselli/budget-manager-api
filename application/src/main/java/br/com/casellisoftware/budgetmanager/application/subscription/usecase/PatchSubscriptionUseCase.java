package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInputAssembler;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionPatch;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

public class PatchSubscriptionUseCase implements PatchSubscriptionBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchSubscriptionUseCase.class);

    private final SubscriptionRepository subscriptionRepository;
    private final Clock clock;

    public PatchSubscriptionUseCase(SubscriptionRepository subscriptionRepository, Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.clock = clock;
    }

    @Override
    public SubscriptionOutput execute(PatchSubscriptionInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Patching subscription id={}", input.id());

        Subscription existing = subscriptionRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new SubscriptionNotFoundException(input.id()));

        SubscriptionPatch patch = PatchSubscriptionInputAssembler.toPatch(input, existing.getCurrency());
        YearMonth effectiveMonth = input.effectiveMonth() != null ? input.effectiveMonth() : YearMonth.now(clock);
        Subscription patched = existing.applyPatch(patch, effectiveMonth);
        Subscription saved = subscriptionRepository.save(patched);
        log.info("Subscription patched successfully, id={}", saved.getId());

        return SubscriptionOutputAssembler.from(saved);
    }
}
