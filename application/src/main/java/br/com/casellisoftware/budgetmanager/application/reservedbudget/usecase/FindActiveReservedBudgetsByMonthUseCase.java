package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindActiveReservedBudgetsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class FindActiveReservedBudgetsByMonthUseCase implements FindActiveReservedBudgetsByMonthBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindActiveReservedBudgetsByMonthUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;

    public FindActiveReservedBudgetsByMonthUseCase(ReservedBudgetRepository reservedBudgetRepository) {
        this.reservedBudgetRepository = reservedBudgetRepository;
    }

    @Override
    public List<ReservedBudgetOutput> execute(YearMonth month, String ownerId) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        log.debug("Finding active reserved budgets for month={} ownerId={}", targetMonth, ownerId);

        return reservedBudgetRepository.findActiveFor(targetMonth, ownerId)
                .stream()
                .map(ReservedBudgetOutputAssembler::from)
                .toList();
    }
}
