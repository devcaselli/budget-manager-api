package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInputAssembler;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetPatch;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Objects;

public class PatchReservedBudgetUseCase implements PatchReservedBudgetBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchReservedBudgetUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;
    private final Clock clock;
    private final ReservedBudgetLinkValidationService linkValidationService;

    /**
     * Constructor without cap validation (backwards-compatible; cap validation skipped
     * when no links are present regardless).
     */
    public PatchReservedBudgetUseCase(ReservedBudgetRepository reservedBudgetRepository, Clock clock) {
        this(reservedBudgetRepository, clock, null);
    }

    /**
     * Constructor with cap validation support. When {@code linkValidationService} is
     * non-null and the patch changes the amount, cap invariant is re-validated against
     * existing links before saving.
     */
    public PatchReservedBudgetUseCase(ReservedBudgetRepository reservedBudgetRepository,
                                      Clock clock,
                                      ReservedBudgetLinkValidationService linkValidationService) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository, "reservedBudgetRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.linkValidationService = linkValidationService; // nullable: validation disabled when null
    }

    @Override
    public ReservedBudgetOutput execute(PatchReservedBudgetInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Patching reserved budget id={}", input.id());

        ReservedBudget existing = reservedBudgetRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new ReservedBudgetNotFoundException(input.id()));

        ReservedBudgetPatch patch = PatchReservedBudgetInputAssembler.toPatch(input, existing.getCurrency());
        YearMonth effectiveMonth = input.effectiveMonth() != null ? input.effectiveMonth() : YearMonth.now(clock);
        ReservedBudget patched = existing.applyPatch(patch, effectiveMonth);

        // Re-validate cap if the amount changed and the RB has links.
        // This catches the "down-edit: new ceiling < existing linked sum" scenario.
        if (linkValidationService != null
                && patch.newAmount().isPresent()
                && !patched.getLinks().isEmpty()) {
            linkValidationService.validate(patched, patched.getLinks(), input.ownerId());
        }

        ReservedBudget saved = reservedBudgetRepository.save(patched);
        log.info("Reserved budget patched successfully, id={}", saved.getId());

        return ReservedBudgetOutputAssembler.from(saved);
    }
}
