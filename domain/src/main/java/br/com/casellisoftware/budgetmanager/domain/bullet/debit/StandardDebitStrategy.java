package br.com.casellisoftware.budgetmanager.domain.bullet.debit;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

public final class StandardDebitStrategy implements DebitStrategy {

    @Override
    public Money applyDebit(Bullet current, Money amount) {
        return current.getRemaining().debitBy(amount);
    }
}
