package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Objects;

public class SaveReservedBudgetUseCase implements SaveReservedBudgetBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveReservedBudgetUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;
    private final Clock clock;

    public SaveReservedBudgetUseCase(ReservedBudgetRepository reservedBudgetRepository, Clock clock) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository, "reservedBudgetRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ReservedBudgetOutput execute(ReservedBudgetInput input) {
        Objects.requireNonNull(input, "input must not be null");
        YearMonth effectiveMonth = input.effectiveMonth() != null ? input.effectiveMonth() : YearMonth.now(clock);
        Currency currency = Currency.getInstance(Objects.requireNonNull(input.currency(), "currency must not be null"));
        Money amount = Money.of(input.budget(), currency);

        log.info("Saving reserved budget for effectiveMonth={} ownerId={}", effectiveMonth, input.ownerId());
        ReservedBudget reservedBudget = ReservedBudget.create(
                input.description(),
                input.details(),
                currency,
                amount,
                effectiveMonth,
                input.flag(),
                input.ownerId()
        );
        ReservedBudget saved = reservedBudgetRepository.save(reservedBudget);
        log.info("Reserved budget saved id={}", saved.getId());

        return ReservedBudgetOutputAssembler.from(saved);
    }
}
