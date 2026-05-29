package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.util.Collection;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InstallmentRepository {

    Installment save(Installment installment);

    Optional<Installment> findById(String id);
    default Optional<Installment> findById(String id, String ownerId) {
        return findById(id).filter(installment -> installment.getOwnerId().equals(ownerId));
    }

    Map<String, Installment> findAllByIds(Collection<String> ids, String ownerId);

    boolean existsById(String id);
    default boolean existsById(String id, String ownerId) {
        return findById(id, ownerId).isPresent();
    }

    /**
     * Installments whose payment window covers the given month and are not deleted.
     *
     * <p>Query semantics depend on the installment's source:
     * <ul>
     *   <li><b>Standalone</b> ({@code sourceWalletId == null}): the first parcel
     *   is charged in {@code sourceEffectiveMonth} itself, so the source month is
     *   inclusive — {@code sourceEffectiveMonth <= walletMonth}.</li>
     *   <li><b>From-expense</b> ({@code sourceWalletId != null}): the purchase was
     *   already counted in the source wallet's month, so the first parcel
     *   materializes the month after — {@code sourceEffectiveMonth < walletMonth}.</li>
     * </ul>
     * Both branches also require {@code deleted == false} and
     * {@code lastInstallmentDate >= walletMonth}.</p>
     */
    List<Installment> findActiveAffecting(YearMonth walletMonth);
    default List<Installment> findActiveAffecting(YearMonth walletMonth, String ownerId) {
        return findActiveAffecting(walletMonth).stream()
                .filter(installment -> installment.getOwnerId().equals(ownerId))
                .toList();
    }

    /**
     * Returns active (non-deleted) installments that affect at least one of the given months.
     */
    List<Installment> findActiveAffectingAny(Collection<YearMonth> walletMonths);
    default List<Installment> findActiveAffectingAny(Collection<YearMonth> walletMonths, String ownerId) {
        return findActiveAffectingAny(walletMonths).stream()
                .filter(installment -> installment.getOwnerId().equals(ownerId))
                .toList();
    }

    /**
     * Returns active (non-deleted) installments whose source wallet matches.
     */
    List<Installment> findBySourceWalletIdAndNotDeleted(String sourceWalletId);
    default List<Installment> findBySourceWalletIdAndNotDeleted(String sourceWalletId, String ownerId) {
        return findBySourceWalletIdAndNotDeleted(sourceWalletId).stream()
                .filter(installment -> installment.getOwnerId().equals(ownerId))
                .toList();
    }

    List<String> findIdsByCreditCardId(String creditCardId);
    List<String> findIdsByCreditCardId(String creditCardId, String ownerId);

    List<String> findIdsByCreditCardIdAndNotDeleted(String creditCardId);
    List<String> findIdsByCreditCardIdAndNotDeleted(String creditCardId, String ownerId);

    /**
     * Unified wallet-context query combining "source wallet" and "active affecting" installments
     * into a single paginated, optionally-filtered, sorted result set.
     *
     * <p>Semantics: {@code ownerId = :ownerId AND deleted = false AND
     * (sourceWalletId = :walletId OR (sourceEffectiveMonth < :effectiveMonth AND lastInstallmentDate >= :effectiveMonth))
     * [AND creditCardId = :creditCardId]}
     * ORDER BY lastInstallmentDate ASC|DESC
     * </p>
     *
     * @param walletId        the wallet whose installments to retrieve
     * @param effectiveMonth  the wallet's effective month for the "active affecting" branch
     * @param creditCardId    optional credit-card filter; {@code null} means no filter
     * @param sortOrder       sort direction on {@code lastInstallmentDate}
     * @param page            zero-based page index
     * @param size            page size
     * @param ownerId         owner for multi-tenant isolation
     */
    PageResult<Installment> findByWalletContext(String walletId,
                                                YearMonth effectiveMonth,
                                                String creditCardId,
                                                InstallmentSortOrder sortOrder,
                                                int page,
                                                int size,
                                                String ownerId);

    /**
     * Unified wallet-context query combining "source wallet" and "active affecting" installments
     * into a single optionally-filtered, sorted result set (non-paginated).
     *
     * <p>Semantics: {@code ownerId = :ownerId AND deleted = false AND
     * (sourceWalletId = :walletId OR (sourceEffectiveMonth < :effectiveMonth AND lastInstallmentDate >= :effectiveMonth))
     * [AND creditCardId = :creditCardId]}
     * ORDER BY lastInstallmentDate ASC|DESC
     * </p>
     *
     * @param walletId        the wallet whose installments to retrieve
     * @param effectiveMonth  the wallet's effective month for the "active affecting" branch
     * @param creditCardId    optional credit-card filter; {@code null} means no filter
     * @param sortOrder       sort direction on {@code lastInstallmentDate}
     * @param ownerId         owner for multi-tenant isolation
     */
    List<Installment> findByWalletContext(String walletId,
                                          YearMonth effectiveMonth,
                                          String creditCardId,
                                          InstallmentSortOrder sortOrder,
                                          String ownerId);

    PageResult<Installment> findAll(int page, int size);
    PageResult<Installment> findAll(int page, int size, String ownerId);
}
