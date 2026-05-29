package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the best PRODUCTION wallet for ingest-sync expense creation.
 *
 * <p>Priority rules (first match wins), all wallets scoped to {@code ownerId} and
 * {@code PRODUCTION} state, evaluated against {@code today}:</p>
 * <ol>
 *   <li>Current-month open: {@code effectiveMonth == YearMonth(today)}, {@code closed=false}</li>
 *   <li>Future open: {@code effectiveMonth > YearMonth(today)}, {@code closed=false}, smallest effectiveMonth</li>
 *   <li>Current-month closed: {@code effectiveMonth == YearMonth(today)}, {@code closed=true}</li>
 *   <li>Past closed most-recent: {@code effectiveMonth < YearMonth(today)}, largest effectiveMonth</li>
 *   <li>Any wallet (fallback): most-recent by effectiveMonth</li>
 * </ol>
 *
 * @implNote Time complexity: O(n), Space complexity: O(1) where n = PRODUCTION wallets for owner.
 */
public class ResolveIngestWalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveIngestWalletUseCase.class);

    private final WalletRepository walletRepository;

    public ResolveIngestWalletUseCase(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
    }

    /**
     * Resolves the best wallet for an ingest expense.
     *
     * @param ownerId owner identifier
     * @param today   current date in the owner's timezone (America/Sao_Paulo)
     * @return resolved wallet or empty if no PRODUCTION wallet exists for this owner
     */
    public Optional<Wallet> resolve(String ownerId, java.time.LocalDate today) {
        List<Wallet> wallets = walletRepository.findAllProductionByOwnerId(ownerId);
        if (wallets.isEmpty()) {
            log.warn("No PRODUCTION wallets found for ownerId={}", ownerId);
            return Optional.empty();
        }

        YearMonth currentMonth = YearMonth.from(today);

        // Priority 1: current-month open
        Optional<Wallet> p1 = wallets.stream()
                .filter(w -> !w.isClosed() && w.getEffectiveMonth().equals(currentMonth))
                .findFirst();
        if (p1.isPresent()) return p1;

        // Priority 2: future open — smallest effectiveMonth
        Optional<Wallet> p2 = wallets.stream()
                .filter(w -> !w.isClosed() && w.getEffectiveMonth().isAfter(currentMonth))
                .min(Comparator.comparing(w -> w.getEffectiveMonth()));
        if (p2.isPresent()) return p2;

        // Priority 3: current-month closed
        Optional<Wallet> p3 = wallets.stream()
                .filter(w -> w.isClosed() && w.getEffectiveMonth().equals(currentMonth))
                .findFirst();
        if (p3.isPresent()) return p3;

        // Priority 4: past closed most-recent
        Optional<Wallet> p4 = wallets.stream()
                .filter(w -> w.isClosed() && w.getEffectiveMonth().isBefore(currentMonth))
                .max(Comparator.comparing(w -> w.getEffectiveMonth()));
        if (p4.isPresent()) return p4;

        // Priority 5: any — most recent by effectiveMonth
        return wallets.stream()
                .max(Comparator.comparing(w -> w.getEffectiveMonth()));
    }
}
