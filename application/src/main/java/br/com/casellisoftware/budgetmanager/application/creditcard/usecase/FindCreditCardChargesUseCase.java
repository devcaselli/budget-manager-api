package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardChargesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardChargesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardResult;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FindCreditCardChargesUseCase implements FindCreditCardChargesBoundary {

    private static final int LARGE_PAGE = 1000;

    private final CreditCardRepository creditCardRepository;
    private final ExpenseRepository expenseRepository;
    private final WalletRepository walletRepository;
    private final InstallmentRepository installmentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ShareRepository shareRepository;

    public FindCreditCardChargesUseCase(CreditCardRepository creditCardRepository,
                                        ExpenseRepository expenseRepository,
                                        WalletRepository walletRepository,
                                        InstallmentRepository installmentRepository,
                                        SubscriptionRepository subscriptionRepository,
                                        ShareRepository shareRepository) {
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository, "creditCardRepository must not be null");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public CreditCardChargesOutput execute(String creditCardId, YearMonth effectiveMonth, String ownerId) {
        Objects.requireNonNull(creditCardId, "creditCardId must not be null");
        Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        creditCardRepository.findById(creditCardId, ownerId)
                .orElseThrow(() -> new CreditCardNotFoundException(creditCardId));

        List<ExpenseOutput> expenses = resolveExpenses(creditCardId, effectiveMonth, ownerId);
        List<InstallmentOutput> installments = resolveInstallments(creditCardId, effectiveMonth, ownerId);
        List<SubscriptionChargeOutput> subscriptions = resolveSubscriptions(creditCardId, effectiveMonth, ownerId);

        BigDecimal totalCost = sum(expenses, ExpenseOutput::cost)
                .add(sum(installments, InstallmentOutput::effectiveInstallmentValue))
                .add(sum(subscriptions, SubscriptionChargeOutput::amount));

        return new CreditCardChargesOutput(expenses, installments, subscriptions, totalCost);
    }

    private List<ExpenseOutput> resolveExpenses(String creditCardId, YearMonth month, String ownerId) {
        List<String> walletIds = walletRepository.findIdsByEffectiveMonth(month, ownerId);
        if (walletIds.isEmpty()) {
            return List.of();
        }
        ExpenseByCreditCardResult result = expenseRepository.findByCreditCardId(
                creditCardId,
                new ExpenseByCreditCardFilter(walletIds, null),
                0,
                LARGE_PAGE,
                ownerId
        );
        return result.expenses().content().stream()
                .map(ExpenseOutputAssembler::from)
                .toList();
    }

    private List<InstallmentOutput> resolveInstallments(String creditCardId, YearMonth month, String ownerId) {
        List<Installment> affecting = installmentRepository.findActiveAffecting(month, ownerId).stream()
                .filter(installment -> creditCardId.equals(installment.getCreditCardId()))
                .toList();
        if (affecting.isEmpty()) {
            return List.of();
        }
        Map<String, Share> sharesByInstallmentId = shareRepository.findActiveBySourceIds(
                ShareSourceType.INSTALLMENT,
                affecting.stream().map(Installment::getId).toList(),
                ownerId);
        return affecting.stream()
                .map(installment -> InstallmentOutputAssembler.from(installment, sharesByInstallmentId.get(installment.getId())))
                .toList();
    }

    private List<SubscriptionChargeOutput> resolveSubscriptions(String creditCardId, YearMonth month, String ownerId) {
        List<Subscription> active = subscriptionRepository.findActiveForByOwnerId(month, ownerId).stream()
                .filter(subscription -> creditCardId.equals(subscription.getCreditCardId()))
                .toList();
        if (active.isEmpty()) {
            return List.of();
        }
        Map<String, Share> sharesBySubscriptionId = shareRepository.findActiveBySourceIds(
                ShareSourceType.SUBSCRIPTION,
                active.stream().map(Subscription::getId).toList(),
                ownerId);
        return active.stream()
                .map(subscription -> {
                    SubscriptionChargeOutput preview = SubscriptionChargeOutputAssembler.preview(subscription, month);
                    Share share = sharesBySubscriptionId.get(subscription.getId());
                    if (share == null) {
                        return preview;
                    }
                    BigDecimal scaledAmount = preview.amount()
                            .multiply(share.getOwnerRatio())
                            .setScale(2, RoundingMode.HALF_EVEN);
                    return new SubscriptionChargeOutput(
                            preview.id(),
                            preview.subscriptionId(),
                            preview.walletId(),
                            preview.month(),
                            scaledAmount,
                            preview.remaining(),
                            preview.flag(),
                            true,
                            scaledAmount
                    );
                })
                .toList();
    }

    private <T> BigDecimal sum(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
