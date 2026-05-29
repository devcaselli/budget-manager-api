package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentInput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentInputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentPatch;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class PatchInstallmentUseCase implements PatchInstallmentBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchInstallmentUseCase.class);

    private final InstallmentRepository installmentRepository;
    private final FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    public PatchInstallmentUseCase(InstallmentRepository installmentRepository,
                                   FindCreditCardByIdBoundary findCreditCardByIdBoundary) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.findCreditCardByIdBoundary = Objects.requireNonNull(findCreditCardByIdBoundary, "findCreditCardByIdBoundary must not be null");
    }

    @Override
    public InstallmentOutput execute(PatchInstallmentInput input) {
        log.info("Patching installment id={}", input.id());

        Installment existing = installmentRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new InstallmentNotFoundException(input.id()));

        // Validate new creditCardId belongs to owner before applying patch
        if (input.creditCardId() != null) {
            findCreditCardByIdBoundary.findById(input.creditCardId(), input.ownerId());
        }

        InstallmentPatch patch = PatchInstallmentInputAssembler.toPatch(input, existing);
        if (log.isDebugEnabled()) {
            log.debug("Applying installment patch id={}, fields={}", input.id(), patch.appliedFieldNames());
        }
        Installment patched = existing.patch(patch);
        Installment saved = installmentRepository.save(patched);
        log.info("Installment patched successfully, id={}", saved.getId());

        return InstallmentOutputAssembler.from(saved);
    }
}
