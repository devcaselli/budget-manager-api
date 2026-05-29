package br.com.casellisoftware.budgetmanager.domain.extrabudget;

import java.util.List;
import java.util.Optional;

public interface ExtraBudgetRepository {

    ExtraBudget save(ExtraBudget extraBudget);

    Optional<ExtraBudget> findById(String id);

    default Optional<ExtraBudget> findById(String id, String ownerId) {
        return findById(id).filter(e -> e.getOwnerId().equals(ownerId));
    }

    List<ExtraBudget> findByWalletId(String walletId, String ownerId);

    List<ExtraBudget> findByBulletId(String bulletId, String ownerId);
}
