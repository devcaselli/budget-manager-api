package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes the payer's monthly + journey amounts across every active share
 * referencing them.
 *
 * <p>For each active share found via
 * {@link ShareRepository#findActiveByPayerId(String, String)} the payer's
 * quota ratio is read from the share. Then:
 * <ul>
 *   <li>EXPENSE → monthly = journey = {@code share.totalAmount * ratio}.</li>
 *   <li>INSTALLMENT → monthly = {@code installment.installmentValue * ratio};
 *   journey = {@code share.totalAmount * ratio} (the full debt).</li>
 *   <li>SUBSCRIPTION → monthly = journey = {@code share.totalAmount * ratio}
 *   (recurring; the share owns the journey while it's active).</li>
 * </ul>
 *
 * <p>Values are rounded HALF_EVEN to 2 decimals and accumulated in the
 * share's currency. Mixed-currency setups raise {@link IllegalStateException}.</p>
 */
public class PayerAmountDueCalculator {

    private static final int DISPLAY_SCALE = 2;

    private final ShareRepository shareRepository;
    private final InstallmentRepository installmentRepository;

    public PayerAmountDueCalculator(ShareRepository shareRepository,
                                    InstallmentRepository installmentRepository) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
    }

    public PayerAmountDue calculate(Payer payer, String ownerId) {
        Objects.requireNonNull(payer, "payer must not be null");
        String resolvedOwnerId = Objects.requireNonNull(ownerId, "ownerId must not be null");

        List<Share> activeShares = shareRepository.findActiveByPayerId(payer.getId(), resolvedOwnerId);
        if (activeShares.isEmpty()) {
            return PayerAmountDue.zero();
        }
        Map<String, Installment> installmentsById = installmentRepository.findAllByIds(
                activeShares.stream()
                        .filter(share -> share.getSourceType() == ShareSourceType.INSTALLMENT)
                        .map(Share::getSourceId)
                        .toList(),
                resolvedOwnerId
        );

        Money monthlyTotal = null;
        Money journeyTotal = null;
        for (Share share : activeShares) {
            BigDecimal ratio = ratioForPayer(share, payer.getId());
            if (ratio.signum() == 0) {
                continue;
            }
            Money journey = scaledMoney(share.getTotalAmount(), ratio);
            Money monthly = monthlyFor(share, ratio, installmentsById);
            monthlyTotal = (monthlyTotal == null) ? monthly : addSameCurrency(monthlyTotal, monthly);
            journeyTotal = (journeyTotal == null) ? journey : addSameCurrency(journeyTotal, journey);
        }
        if (monthlyTotal == null) {
            return PayerAmountDue.zero();
        }
        return new PayerAmountDue(monthlyTotal, journeyTotal);
    }

    private Money monthlyFor(Share share, BigDecimal ratio, Map<String, Installment> installmentsById) {
        if (share.getSourceType() == ShareSourceType.INSTALLMENT) {
            Installment installment = installmentsById.get(share.getSourceId());
            if (installment != null) {
                return scaledMoney(installment.getInstallmentValue(), ratio);
            }
        }
        return scaledMoney(share.getTotalAmount(), ratio);
    }

    private BigDecimal ratioForPayer(Share share, String payerId) {
        return share.getQuotas().stream()
                .filter(q -> payerId.equals(q.payerId()))
                .map(ShareQuota::ratio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Money scaledMoney(Money base, BigDecimal ratio) {
        BigDecimal scaled = base.amount()
                .multiply(ratio)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_EVEN);
        return Money.of(scaled, base.currency());
    }

    private Money addSameCurrency(Money left, Money right) {
        if (!left.currency().equals(right.currency())) {
            throw new IllegalStateException(
                    "currency mismatch while calculating payer amountDue: "
                            + left.currency().getCurrencyCode()
                            + " vs "
                            + right.currency().getCurrencyCode());
        }
        return left.add(right);
    }
}
