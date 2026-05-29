package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;

import java.util.List;

public interface FindInstallmentsByWalletIdBoundary {

    PageResult<InstallmentOutput> execute(String walletId, InstallmentWalletFilter filter, String ownerId);

    List<InstallmentOutput> executeAll(String walletId, String creditCardId, InstallmentSortOrder sortOrder, String ownerId);
}
