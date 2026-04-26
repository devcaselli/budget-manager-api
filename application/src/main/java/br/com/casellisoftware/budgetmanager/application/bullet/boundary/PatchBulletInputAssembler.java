package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

/**
 * Bridges the application patch contract to the domain-native {@link BulletPatch},
 * keeping the aggregate free from application DTO dependencies.
 */
public final class PatchBulletInputAssembler {

    private PatchBulletInputAssembler() {
    }

    public static BulletPatch toPatch(PatchBulletInput input) {
        return BulletPatch.empty()
                .withDescription(input.description())
                .withBudget(input.budget() == null ? null : Money.of(input.budget()))
                .withRemaining(input.remaining() == null ? null : Money.of(input.remaining()))
                .withWalletId(input.walletId());
    }
}
