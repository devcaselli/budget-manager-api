package br.com.casellisoftware.budgetmanager.domain.bullet.debit;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

public interface DebitStrategy {

    Money applyDebit(Bullet current, Money amount);
}
