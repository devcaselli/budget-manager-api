package br.com.casellisoftware.budgetmanager.domain.expense;

import java.util.List;

public record ExpenseByCreditCardFilter(
        List<String> walletIds,
        String name
) {
}
