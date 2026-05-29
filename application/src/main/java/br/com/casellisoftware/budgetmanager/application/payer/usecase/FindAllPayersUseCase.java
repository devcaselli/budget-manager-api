package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindAllPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;

import java.util.List;
import java.util.Objects;

public class FindAllPayersUseCase implements FindAllPayersBoundary {

    private final PayerRepository payerRepository;
    private final PayerAmountDueCalculator calculator;

    public FindAllPayersUseCase(PayerRepository payerRepository, PayerAmountDueCalculator calculator) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public List<PayerOutput> execute(String ownerId) {
        // TODO payer-bulk-amount-due: replace P x query loop with a bulk lookup when payer volume grows.
        // Only STANDING payers are visible in the global listing. TRANSIENTs are
        // scoped to a single wallet and must be requested via the wallet-scoped
        // endpoint (GET /wallets/{walletId}/payers) so the UI never surfaces
        // a transient outside its origin wallet.
        return payerRepository.findAllStanding(ownerId)
                .stream()
                .map(payer -> PayerOutputAssembler.from(payer, calculator.calculate(payer, ownerId)))
                .toList();
    }
}
