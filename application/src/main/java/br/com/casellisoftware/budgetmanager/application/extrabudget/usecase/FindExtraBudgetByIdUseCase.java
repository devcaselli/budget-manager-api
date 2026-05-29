package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class FindExtraBudgetByIdUseCase implements FindExtraBudgetByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindExtraBudgetByIdUseCase.class);

    private final ExtraBudgetRepository extraBudgetRepository;

    public FindExtraBudgetByIdUseCase(ExtraBudgetRepository extraBudgetRepository) {
        this.extraBudgetRepository = Objects.requireNonNull(extraBudgetRepository, "extraBudgetRepository must not be null");
    }

    @Override
    public ExtraBudgetOutput execute(String id, String ownerId) {
        log.debug("Finding extra budget by id={} ownerId={}", id, ownerId);

        ExtraBudget extraBudget = extraBudgetRepository.findById(id, ownerId)
                .orElseThrow(() -> new ExtraBudgetNotFoundException(id));

        if (extraBudget.isDeleted()) {
            log.debug("ExtraBudget id={} is soft-deleted, treating as not found", id);
            throw new ExtraBudgetNotFoundException(id);
        }

        log.debug("ExtraBudget found, id={}", extraBudget.getId());
        return ExtraBudgetOutputAssembler.from(extraBudget);
    }
}
