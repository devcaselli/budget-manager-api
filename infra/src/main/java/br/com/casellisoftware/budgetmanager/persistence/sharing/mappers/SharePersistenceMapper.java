package br.com.casellisoftware.budgetmanager.persistence.sharing.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import br.com.casellisoftware.budgetmanager.persistence.sharing.ShareDocument;
import br.com.casellisoftware.budgetmanager.persistence.sharing.ShareQuotaDocument;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.List;

@Component
public class SharePersistenceMapper {

    public ShareDocument toDocument(Share share, Long version) {
        if (share == null) {
            return null;
        }
        return new ShareDocument(
                share.getId(),
                share.getOwnerId(),
                share.getWalletId(),
                share.getSourceType().name(),
                share.getSourceId(),
                share.getTotalAmount().amount(),
                share.getTotalAmount().currency().getCurrencyCode(),
                share.getOwnerShare().amount(),
                share.getOwnerShare().currency().getCurrencyCode(),
                share.getOwnerRatio(),
                share.getQuotas().stream().map(this::toQuotaDocument).toList(),
                share.getStatus().name(),
                share.getPaymentIds(),
                share.getCreatedAt(),
                share.getRevertedAt(),
                version
        );
    }

    public Share toDomain(ShareDocument document) {
        if (document == null) {
            return null;
        }
        return new Share(
                document.getId(),
                document.getOwnerId(),
                document.getWalletId(),
                ShareSourceType.valueOf(document.getSourceType()),
                document.getSourceId(),
                Money.of(document.getTotalAmount(), Currency.getInstance(document.getTotalCurrency())),
                Money.of(document.getOwnerShare(), Currency.getInstance(document.getOwnerCurrency())),
                document.getOwnerRatio(),
                document.getQuotas().stream().map(this::toQuotaDomain).toList(),
                ShareStatus.valueOf(document.getStatus()),
                document.getPaymentIds(),
                document.getCreatedAt(),
                document.getRevertedAt()
        );
    }

    private ShareQuotaDocument toQuotaDocument(ShareQuota quota) {
        return new ShareQuotaDocument(
                quota.payerId(),
                quota.ratio(),
                quota.paymentIds()
        );
    }

    private ShareQuota toQuotaDomain(ShareQuotaDocument quotaDocument) {
        return new ShareQuota(
                quotaDocument.getPayerId(),
                quotaDocument.getRatio(),
                quotaDocument.getPaymentIds() == null ? List.of() : quotaDocument.getPaymentIds()
        );
    }
}
