package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class FindExtraBudgetsByWalletIdUseCase implements FindExtraBudgetsByWalletIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindExtraBudgetsByWalletIdUseCase.class);

    private final ExtraBudgetRepository extraBudgetRepository;

    public FindExtraBudgetsByWalletIdUseCase(ExtraBudgetRepository extraBudgetRepository) {
        this.extraBudgetRepository = Objects.requireNonNull(extraBudgetRepository, "extraBudgetRepository must not be null");
    }

    @Override
    public List<ExtraBudgetOutput> execute(String walletId, String ownerId) {
        log.debug("Finding extra budgets by walletId={} ownerId={}", walletId, ownerId);

        return extraBudgetRepository.findByWalletId(walletId, ownerId).stream()
                .map(ExtraBudgetOutputAssembler::from)
                .toList();
    }
}
