package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Objects;

public class SaveSubscriptionUseCase implements SaveSubscriptionBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveSubscriptionUseCase.class);

    private final SubscriptionRepository subscriptionRepository;
    private final CreditCardRepository creditCardRepository;
    private final Clock clock;

    public SaveSubscriptionUseCase(SubscriptionRepository subscriptionRepository,
                                   CreditCardRepository creditCardRepository,
                                   Clock clock) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository, "creditCardRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SubscriptionOutput execute(SubscriptionInput input) {
        Objects.requireNonNull(input, "input must not be null");
        YearMonth effectiveMonth = input.effectiveMonth() != null ? input.effectiveMonth() : YearMonth.now(clock);
        SubscriptionState state = input.state() == null ? SubscriptionState.PRODUCTION : input.state();
        Currency currency = Currency.getInstance(Objects.requireNonNull(input.currency(), "currency must not be null"));
        Money amount = Money.of(input.amount(), currency);
        String creditCardId = Objects.requireNonNull(input.creditCardId(), "creditCardId must not be null");

        if (!creditCardRepository.existsById(creditCardId, input.ownerId())) {
            throw new CreditCardNotFoundException(creditCardId);
        }

        log.info("Saving subscription for effectiveMonth={} state={} creditCardId={}", effectiveMonth, state, creditCardId);
        Subscription subscription = Subscription.create(input.description(), currency, amount, effectiveMonth, state, input.flag(), input.ownerId(), creditCardId);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription saved id={}", saved.getId());

        return SubscriptionOutputAssembler.from(saved);
    }
}
