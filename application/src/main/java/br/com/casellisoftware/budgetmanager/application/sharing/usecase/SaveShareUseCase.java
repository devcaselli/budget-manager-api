package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.usecase.EnsureTransientPayerUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.TransientPayerSpec;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareQuotaInput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareAlreadyActiveForSourceException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareCurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRatioMismatchException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveShareUseCase implements SaveShareBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveShareUseCase.class);
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    private final ShareRepository shareRepository;
    private final ExpenseRepository expenseRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final PaymentRepository paymentRepository;
    private final PayerRepository payerRepository;
    private final EnsureTransientPayerUseCase ensureTransientPayerUseCase;
    private final Clock clock;

    public SaveShareUseCase(ShareRepository shareRepository,
                            ExpenseRepository expenseRepository,
                            SubscriptionRepository subscriptionRepository,
                            InstallmentRepository installmentRepository,
                            PaymentRepository paymentRepository,
                            PayerRepository payerRepository,
                            EnsureTransientPayerUseCase ensureTransientPayerUseCase,
                            Clock clock) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.ensureTransientPayerUseCase = Objects.requireNonNull(ensureTransientPayerUseCase, "ensureTransientPayerUseCase must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ShareOutput execute(ShareInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Instant now = Instant.now(clock);
        if (shareRepository.existsActiveBySourceId(input.sourceType(), input.sourceId(), input.ownerId())) {
            throw new ShareAlreadyActiveForSourceException(input.sourceType(), input.sourceId());
        }

        Currency currency = resolveCurrency(input.currency());
        Money totalAmount = Money.of(input.totalAmount(), currency);
        Money ownerShare = Money.of(input.ownerShare(), currency);
        Money sourceTotal = resolveSourceTotal(input);
        validateSourceTotal(sourceTotal, totalAmount, input.sourceType(), input.sourceId());

        List<Share.ShareQuotaAllocation> quotaAllocations = resolveQuotaAllocations(input, currency);
        Share created = Share.create(
                input.walletId(),
                input.sourceType(),
                input.sourceId(),
                totalAmount,
                ownerShare,
                quotaAllocations,
                input.ownerId(),
                now
        );

        Share savedShare = shareRepository.save(created);

        boolean isExpenseSource = input.sourceType() == br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.EXPENSE;
        Expense expenseAccumulator = isExpenseSource ? findExpense(input.sourceId(), input.ownerId()) : null;
        String expenseIdForPayment = isExpenseSource ? input.sourceId() : null;

        Instant paymentDate = now;

        // NOTE: no owner-payment is emitted here. The owner must still pay their portion
        // of the expense through the regular Pay flow (consuming wallet bullets). The
        // share only registers the split and settles the payers' quotas so the expense's
        // remaining reflects what the payers already covered.
        Map<String, String> paymentByPayerId = new LinkedHashMap<>();
        for (Share.ShareQuotaAllocation allocation : quotaAllocations) {
            Payment payerPayment = Payment.createShared(
                    allocation.amount(),
                    paymentDate,
                    "shared payer quota for " + allocation.payerId(),
                    expenseIdForPayment,
                    input.walletId(),
                    null,
                    allocation.payerId(),
                    savedShare.getId(),
                    FlagEnum.NONE,
                    input.ownerId()
            );
            Payment savedPayerPayment = paymentRepository.save(payerPayment);
            paymentByPayerId.put(allocation.payerId(), savedPayerPayment.getId());
            if (expenseAccumulator != null) {
                expenseAccumulator = expenseAccumulator.pay(savedPayerPayment);
            }
        }

        if (expenseAccumulator != null) {
            if (savedShare.isFullAssignment()) {
                expenseAccumulator = expenseAccumulator.hide();
            }
            expenseRepository.save(expenseAccumulator);
        }

        if (input.sourceType() == br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.INSTALLMENT) {
            cascadeInstallmentExpense(input, savedShare);
        }

        Share shareWithPayments = savedShare.appendPayments(null, paymentByPayerId);
        Share persisted = shareRepository.save(shareWithPayments);
        log.info("share created: shareId={} ownerId={} sourceType={} sourceId={} paymentIds={}",
                persisted.getId(), input.ownerId(), input.sourceType(), input.sourceId(),
                persisted.getPaymentIds());
        return ShareOutputAssembler.from(persisted, payerRepository, input.ownerId());
    }

    /**
     * For "standard" (from-expense) installments, the per-month expense linked
     * to the installment must reflect only the owner's portion after a share is
     * created. We rewrite the prior linked expense in place with
     * {@code installmentValue * ownerRatio} so the wallet's dynamic remaining
     * shows the right amount and the user sees their actual exposure on the
     * expenses screen.
     *
     * <p>Standalone installments ({@code sourceWalletId == null}) have no
     * per-month expense to rewrite — the front renders effective values via
     * {@code InstallmentOutput.effectiveInstallmentValue}, so this method is a
     * no-op for them.</p>
     */
    private void cascadeInstallmentExpense(ShareInput input, Share savedShare) {
        Installment installment = findInstallment(input.sourceId(), input.ownerId());
        if (!installment.hasPerMonthExpense()) {
            return;
        }
        expenseRepository.findByInstallmentId(installment.getId(), input.ownerId())
                .ifPresent(existing -> {
                    Money effectiveInstallmentValue = Money.of(
                            installment.getInstallmentValue().amount()
                                    .multiply(savedShare.getOwnerRatio())
                                    .setScale(installment.getInstallmentValue().amount().scale(),
                                            RoundingMode.HALF_EVEN),
                            installment.getInstallmentValue().currency()
                    );
                    Expense updated = new Expense(
                            existing.getId(),
                            existing.getWalletId(),
                            existing.getCreditCardId(),
                            existing.getName(),
                            effectiveInstallmentValue,
                            effectiveInstallmentValue,
                            existing.getPurchaseDate(),
                            existing.getPaymentIds(),
                            existing.getFlag(),
                            false,
                            installment.getId(),
                            input.ownerId()
                    );
                    expenseRepository.save(updated);
                });
    }

    private List<Share.ShareQuotaAllocation> resolveQuotaAllocations(ShareInput input, Currency currency) {
        if (input.quotas() == null || input.quotas().isEmpty()) {
            throw new IllegalArgumentException("quotas must not be empty");
        }
        List<Share.ShareQuotaAllocation> allocations = new ArrayList<>(input.quotas().size());
        for (ShareQuotaInput quota : input.quotas()) {
            String payerId = resolvePayerId(quota, input.walletId(), input.ownerId(), input.sourceType());
            Money amount = Money.of(Objects.requireNonNull(quota.amount(), "quota amount must not be null"), currency);
            allocations.add(new Share.ShareQuotaAllocation(payerId, amount));
        }
        return allocations;
    }

    private String resolvePayerId(ShareQuotaInput quota,
                                  String walletId,
                                  String ownerId,
                                  br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType sourceType) {
        boolean hasPayerId = quota.payerId() != null && !quota.payerId().isBlank();
        boolean hasTransient = quota.transient_() != null;
        if (hasPayerId == hasTransient) {
            throw new IllegalArgumentException("exactly one of payerId or transient_ must be set");
        }
        boolean isInstallmentShare = sourceType == br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.INSTALLMENT;
        if (hasPayerId) {
            br.com.casellisoftware.budgetmanager.domain.payer.Payer payer = payerRepository.findById(quota.payerId(), ownerId)
                    .orElseThrow(() -> new PayerNotFoundException(quota.payerId()));
            if (isInstallmentShare && payer.getType() == br.com.casellisoftware.budgetmanager.domain.payer.PayerType.TRANSIENT) {
                throw new br.com.casellisoftware.budgetmanager.domain.sharing.TransientPayerNotAllowedForInstallmentException(quota.payerId());
            }
            return quota.payerId();
        }
        // Inline-created payer would always be TRANSIENT — forbidden for installments.
        if (isInstallmentShare) {
            throw new br.com.casellisoftware.budgetmanager.domain.sharing.TransientPayerNotAllowedForInstallmentException("<new-transient>");
        }
        return ensureTransientPayerUseCase.execute(
                new TransientPayerSpec(null, quota.transient_().name(), quota.transient_().paymentDate()),
                walletId,
                ownerId
        );
    }

    private Money resolveSourceTotal(ShareInput input) {
        return switch (input.sourceType()) {
            case EXPENSE -> findExpense(input.sourceId(), input.ownerId()).getCost();
            case SUBSCRIPTION -> findSubscription(input.sourceId(), input.ownerId()).resolveAmount(YearMonth.now(clock));
            case INSTALLMENT -> findInstallment(input.sourceId(), input.ownerId()).getOriginalValue();
        };
    }

    private void validateSourceTotal(Money sourceTotal, Money requestedTotal, br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType sourceType, String sourceId) {
        if (!sourceTotal.currency().equals(requestedTotal.currency())) {
            throw new ShareCurrencyMismatchException(
                    "currency mismatch for source " + sourceType + ":" + sourceId);
        }
        if (sourceTotal.amount().subtract(requestedTotal.amount()).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new ShareRatioMismatchException("requested totalAmount does not match source total amount");
        }
    }

    private Expense findExpense(String expenseId, String ownerId) {
        return expenseRepository.findById(expenseId, ownerId)
                .orElseThrow(() -> new ExpenseNotFoundException(expenseId));
    }

    private Subscription findSubscription(String subscriptionId, String ownerId) {
        return subscriptionRepository.findById(subscriptionId, ownerId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
    }

    private Installment findInstallment(String installmentId, String ownerId) {
        return installmentRepository.findById(installmentId, ownerId)
                .orElseThrow(() -> new InstallmentNotFoundException(installmentId));
    }

    private static Currency resolveCurrency(String currencyCode) {
        try {
            return Currency.getInstance(Objects.requireNonNull(currencyCode, "currency must not be null"));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid currency code: " + currencyCode);
        }
    }
}
