package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.StopWalletShareBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.time.YearMonth;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops a recurring (subscription/installment) share from a given wallet's month
 * onward. Unlike {@code RevertShareUseCase} this is NON-destructive: it does not
 * reverse past payments nor unhide past expenses — it only marks
 * {@code stoppedFromMonth} so that read paths stop applying the owner ratio for
 * wallets at or after that month. Past/closed wallets keep the share for history.
 */
public class StopWalletShareUseCase implements StopWalletShareBoundary {

    private static final Logger log = LoggerFactory.getLogger(StopWalletShareUseCase.class);

    private final ShareRepository shareRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public StopWalletShareUseCase(ShareRepository shareRepository,
                                  FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary, "findWalletDomainByIdBoundary must not be null");
    }

    @Override
    public void execute(String walletId, String shareId, String ownerId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(shareId, "shareId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Wallet wallet = findWalletDomainByIdBoundary.findById(walletId, ownerId);
        YearMonth stopMonth = wallet.getEffectiveMonth();

        Share share = shareRepository.findById(shareId, ownerId)
                .orElseThrow(() -> new ShareNotFoundException(shareId));

        Share stopped = share.stopFrom(stopMonth);
        shareRepository.save(stopped);
        log.info("share stopped from month: shareId={} ownerId={} sourceType={} sourceId={} stoppedFromMonth={}",
                stopped.getId(), ownerId, stopped.getSourceType(), stopped.getSourceId(), stopped.getStoppedFromMonth());
    }
}
