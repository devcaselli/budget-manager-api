package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.math.BigDecimal;
import java.util.List;

public record ShareInput(
        String walletId,
        ShareSourceType sourceType,
        String sourceId,
        BigDecimal totalAmount,
        String currency,
        BigDecimal ownerShare,
        List<ShareQuotaInput> quotas,
        String ownerId
) {
    public ShareInput(String walletId,
                      ShareSourceType sourceType,
                      String sourceId,
                      BigDecimal totalAmount,
                      String currency,
                      BigDecimal ownerShare,
                      List<ShareQuotaInput> quotas) {
        this(walletId, sourceType, sourceId, totalAmount, currency, ownerShare, quotas, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public ShareInput withOwnerId(String ownerId) {
        return new ShareInput(walletId, sourceType, sourceId, totalAmount, currency, ownerShare, quotas, ownerId);
    }
}
