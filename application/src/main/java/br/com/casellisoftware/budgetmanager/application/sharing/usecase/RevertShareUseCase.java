package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevertShareUseCase implements RevertShareBoundary {

    private static final Logger log = LoggerFactory.getLogger(RevertShareUseCase.class);

    private final ShareRepository shareRepository;
    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final InstallmentRepository installmentRepository;
    private final Clock clock;

    public RevertShareUseCase(ShareRepository shareRepository,
                              PaymentRepository paymentRepository,
                              ExpenseRepository expenseRepository,
                              InstallmentRepository installmentRepository,
                              Clock clock) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void execute(String shareId, String ownerId) {
        Share share = shareRepository.findById(shareId, ownerId)
                .orElseThrow(() -> new ShareNotFoundException(shareId));

        boolean isExpenseSource = share.getSourceType() == ShareSourceType.EXPENSE;
        Expense expenseAccumulator = isExpenseSource
                ? expenseRepository.findById(share.getSourceId(), ownerId).orElse(null)
                : null;

        Instant paymentDate = Instant.now(clock);
        for (String paymentId : share.getPaymentIds()) {
            java.util.Optional<Payment> original = paymentRepository.findById(paymentId, ownerId);
            if (original.isEmpty()) {
                continue;
            }
            Payment payment = original.get();
            Payment reversal = Payment.createReversal(
                    payment,
                    paymentDate,
                    "reverted by share " + share.getId(),
                    ownerId
            );
            paymentRepository.save(reversal);
            if (expenseAccumulator != null && payment.getExpenseId() != null) {
                expenseAccumulator = expenseAccumulator.credit(payment.getAmount());
            }
        }

        if (expenseAccumulator != null) {
            if (share.isFullAssignment()) {
                expenseAccumulator = expenseAccumulator.unhide();
            }
            expenseRepository.save(expenseAccumulator);
        }

        if (share.getSourceType() == ShareSourceType.INSTALLMENT) {
            restoreInstallmentExpense(share, ownerId);
        }

        shareRepository.save(share.revert(clock.instant()));
        log.info("share reverted: shareId={} ownerId={} sourceType={} sourceId={} paymentIds={}",
                share.getId(), ownerId, share.getSourceType(), share.getSourceId(), share.getPaymentIds());
    }

    /**
     * Symmetric to {@code SaveShareUseCase#cascadeInstallmentExpense}: rebuild
     * the per-month installment expense at the full {@code installmentValue}
     * once the share is reverted. Standalone installments have no per-month
     * expense — no-op.
     */
    private void restoreInstallmentExpense(Share share, String ownerId) {
        Installment installment = installmentRepository.findById(share.getSourceId(), ownerId)
                .orElseThrow(() -> new InstallmentNotFoundException(share.getSourceId()));
        if (installment.getSourceWalletId() == null) {
            return;
        }
        Expense template = expenseRepository.findByInstallmentId(installment.getId(), ownerId).orElse(null);
        if (template != null) {
            // Restore in-place: preserve expense identity (id, payments, history) and
            // simply revert the financial fields to the full installment value.
            Expense restored = template.restoreAmount(installment.getInstallmentValue());
            expenseRepository.save(restored);
            return;
        }
        Expense replacement = Expense.create(
                installment.getSourceWalletId(),
                installment.getCreditCardId(),
                installment.getDescription(),
                installment.getInstallmentValue(),
                installment.getPurchaseDate(),
                installment.getFlag(),
                false,
                installment.getId(),
                ownerId
        );
        expenseRepository.save(replacement);
    }
}
