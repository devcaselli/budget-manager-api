package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindAllReservedBudgetsBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindAllReservedBudgetsUseCase implements FindAllReservedBudgetsBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindAllReservedBudgetsUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;

    public FindAllReservedBudgetsUseCase(ReservedBudgetRepository reservedBudgetRepository) {
        this.reservedBudgetRepository = reservedBudgetRepository;
    }

    @Override
    public PageResult<ReservedBudgetOutput> execute(int page, int size, String ownerId) {
        log.debug("Finding all reserved budgets page={}, size={}, ownerId={}", page, size, ownerId);

        PageResult<ReservedBudget> reservedBudgetPage = reservedBudgetRepository.findAll(page, size, ownerId);
        return reservedBudgetPage.map(ReservedBudgetOutputAssembler::from);
    }
}
