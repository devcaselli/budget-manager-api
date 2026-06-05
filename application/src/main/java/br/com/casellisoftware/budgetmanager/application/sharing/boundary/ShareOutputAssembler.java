package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ShareOutputAssembler {

    private ShareOutputAssembler() {
    }

    public static ShareOutput from(Share share, PayerRepository payerRepository, String ownerId) {
        Set<String> payerIds = share.getQuotas().stream()
                .map(ShareQuota::payerId)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, String> payerNameById = new HashMap<>();
        if (!payerIds.isEmpty()) {
            List<Payer> payers = payerRepository.findAllByIdsIn(payerIds, ownerId);
            for (Payer payer : payers) {
                payerNameById.put(payer.getId(), payer.getName());
            }
        }
        return from(share, payerNameById);
    }

    public static ShareOutput from(Share share, Map<String, String> payerNameById) {
        List<ShareQuotaOutput> quotaOutputs = share.getQuotas().stream()
                .map(quota -> toQuotaOutput(share, quota, payerNameById))
                .toList();
        return new ShareOutput(
                share.getId(),
                share.getWalletId(),
                share.getSourceType(),
                share.getSourceId(),
                share.getTotalAmount().amount(),
                share.getOwnerShare().amount(),
                share.getOwnerRatio(),
                share.getTotalAmount().currency().getCurrencyCode(),
                share.getStatus(),
                quotaOutputs,
                share.getPaymentIds(),
                share.getCreatedAt(),
                share.getRevertedAt(),
                share.getStoppedFromMonth()
        );
    }

    private static ShareQuotaOutput toQuotaOutput(Share share,
                                                  ShareQuota quota,
                                                  Map<String, String> payerNameById) {
        BigDecimal amount = share.getTotalAmount().amount()
                .multiply(quota.ratio())
                .setScale(Money.SCALE, Money.ROUNDING);
        return new ShareQuotaOutput(
                quota.payerId(),
                payerNameById.get(quota.payerId()),
                quota.ratio(),
                amount,
                quota.paymentIds()
        );
    }
}
