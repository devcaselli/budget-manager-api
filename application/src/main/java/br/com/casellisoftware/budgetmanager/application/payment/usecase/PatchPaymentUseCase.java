package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PatchPaymentBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PatchPaymentInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.shared.PatchHelper;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchPaymentUseCase implements PatchPaymentBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchPaymentUseCase.class);

    private final PaymentRepository paymentRepository;

    public PatchPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentOutput execute(PatchPaymentInput input) {
        log.info("Patching payment id={}", input.id());

        Payment existing = paymentRepository.findById(input.id())
                .orElseThrow(() -> new PaymentNotFoundException(input.id()));

        Payment patched = PatchHelper.applyPatch(existing, input);

        Payment saved = paymentRepository.save(patched);
        log.info("Payment patched successfully, id={}", saved.getId());

        return PaymentOutputAssembler.from(saved);
    }
}
