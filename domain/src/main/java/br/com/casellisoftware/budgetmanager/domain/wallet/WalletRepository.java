package br.com.casellisoftware.budgetmanager.domain.wallet;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findById(String id);
    default Optional<Wallet> findById(String id, String ownerId) {
        return findById(id).filter(wallet -> wallet.getOwnerId().equals(ownerId));
    }
    List<Wallet> findAll();
    default List<Wallet> findAll(String ownerId) {
        return findAll().stream()
                .filter(wallet -> wallet.getOwnerId().equals(ownerId))
                .toList();
    }
    Wallet save(Wallet wallet);
    List<String> findIdsByEffectiveMonth(YearMonth effectiveMonth);
    List<String> findIdsByEffectiveMonth(YearMonth effectiveMonth, String ownerId);

    /**
     * Returns the currently-open PRODUCTION wallet for the given month, if any.
     * "Open" means {@code closed == false} and {@code closedDate} is null or in
     * the future relative to {@code today}.
     */
    Optional<Wallet> findCurrentProductionOpen(YearMonth effectiveMonth, LocalDate today);
    default Optional<Wallet> findCurrentProductionOpen(YearMonth effectiveMonth, LocalDate today, String ownerId) {
        return findCurrentProductionOpen(effectiveMonth, today)
                .filter(wallet -> wallet.getOwnerId().equals(ownerId));
    }

    /**
     * Returns whether an open PRODUCTION wallet exists for the given
     * {@code effectiveMonth}, optionally excluding a wallet by id (useful when
     * patching an existing wallet to PRODUCTION).
     */
    boolean existsOpenProductionFor(YearMonth effectiveMonth, LocalDate today, String excludeId);
    default boolean existsOpenProductionFor(YearMonth effectiveMonth, LocalDate today, String excludeId, String ownerId) {
        return existsOpenProductionFor(effectiveMonth, today, excludeId);
    }

    /**
     * Returns all PRODUCTION wallets for the given owner, ordered by effectiveMonth descending.
     * Used by the ingest-sync wallet resolution strategy which applies a 5-level priority
     * rule in memory.
     *
     * @implNote Time complexity: O(n) where n = number of PRODUCTION wallets for the owner.
     */
    List<Wallet> findAllProductionByOwnerId(String ownerId);
}
