package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;

/**
 * Converts a rich-domain {@link Bullet} into the framework-agnostic
 * {@link BulletOutput} consumed by interface adapters.
 *
 * <p>Hand-written on purpose: the flatten {@code Money → BigDecimal} is trivial,
 * and forcing MapStruct here would require {@code expression} attributes that
 * are uglier than the straight code below.</p>
 */
public final class BulletOutputAssembler {

    private BulletOutputAssembler() {
    }

    public static BulletOutput from(Bullet bullet) {
        return new BulletOutput(
                bullet.getId(),
                bullet.getDescription(),
                bullet.getBudget().amount(),
                bullet.getRemaining().amount(),
                bullet.getWalletId()
        );
    }
}
