package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindReservedBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

public class FindReservedBudgetByIdUseCase implements FindReservedBudgetByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindReservedBudgetByIdUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;
    private final ReservedBudgetConsumptionQuery consumptionQuery;
    private final Clock clock;

    public FindReservedBudgetByIdUseCase(ReservedBudgetRepository reservedBudgetRepository,
                                         ReservedBudgetConsumptionQuery consumptionQuery,
                                         Clock clock) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository);
        this.consumptionQuery = Objects.requireNonNull(consumptionQuery);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ReservedBudgetOutput execute(String id, String ownerId) {
        log.debug("Finding reserved budget by id={} ownerId={}", id, ownerId);

        ReservedBudget reservedBudget = reservedBudgetRepository.findById(id, ownerId)
                .orElseThrow(() -> new ReservedBudgetNotFoundException(id));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption consumption =
                consumptionQuery.consume(reservedBudget, YearMonth.now(clock), ownerId);

        log.debug("Reserved budget found id={}", reservedBudget.getId());
        return ReservedBudgetOutputAssembler.from(
                reservedBudget, consumption.consumed(), consumption.remaining());
    }
}
