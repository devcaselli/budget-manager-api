package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindWalletPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FindWalletPayersUseCase implements FindWalletPayersBoundary {

    private final PayerRepository payerRepository;
    private final PayerAmountDueCalculator calculator;

    public FindWalletPayersUseCase(PayerRepository payerRepository, PayerAmountDueCalculator calculator) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public List<PayerOutput> execute(String walletId, String ownerId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        List<Payer> standing = payerRepository.findAllStanding(ownerId);
        List<Payer> transients = payerRepository.findAllByWalletId(walletId, ownerId);

        Map<String, Payer> byId = new LinkedHashMap<>(standing.size() + transients.size());
        standing.forEach(payer -> byId.put(payer.getId(), payer));
        transients.forEach(payer -> byId.put(payer.getId(), payer));

        List<PayerOutput> outputs = new ArrayList<>(byId.size());
        byId.values().forEach(payer ->
                outputs.add(PayerOutputAssembler.from(payer, calculator.calculate(payer, ownerId))));
        return outputs;
    }
}
