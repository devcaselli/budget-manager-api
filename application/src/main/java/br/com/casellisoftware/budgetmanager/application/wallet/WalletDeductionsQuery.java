package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.util.List;
import java.util.Map;

public interface WalletDeductionsQuery {

    WalletDeductions forWallet(Wallet wallet);

    Map<String, WalletDeductions> forWallets(List<Wallet> wallets);
}
