package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByBulletIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class FindExtraBudgetsByBulletIdUseCase implements FindExtraBudgetsByBulletIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindExtraBudgetsByBulletIdUseCase.class);

    private final ExtraBudgetRepository extraBudgetRepository;

    public FindExtraBudgetsByBulletIdUseCase(ExtraBudgetRepository extraBudgetRepository) {
        this.extraBudgetRepository = Objects.requireNonNull(extraBudgetRepository, "extraBudgetRepository must not be null");
    }

    @Override
    public List<ExtraBudgetOutput> execute(String bulletId, String ownerId) {
        log.debug("Finding extra budgets by bulletId={} ownerId={}", bulletId, ownerId);

        return extraBudgetRepository.findByBulletId(bulletId, ownerId).stream()
                .map(ExtraBudgetOutputAssembler::from)
                .toList();
    }
}
