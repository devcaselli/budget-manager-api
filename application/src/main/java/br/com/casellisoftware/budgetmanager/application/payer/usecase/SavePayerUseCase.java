package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.SavePayerBoundary;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;

import java.util.Objects;

public class SavePayerUseCase implements SavePayerBoundary {

    private final PayerRepository payerRepository;
    private final PayerAmountDueCalculator calculator;

    public SavePayerUseCase(PayerRepository payerRepository, PayerAmountDueCalculator calculator) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public PayerOutput execute(PayerInput input) {
        Objects.requireNonNull(input, "input must not be null");
        Payer payer = Payer.create(
                input.name(),
                input.type(),
                input.walletId(),
                input.subscriptionId(),
                input.paymentDate(),
                input.ownerId());
        Payer saved = payerRepository.save(payer);
        return PayerOutputAssembler.from(saved, calculator.calculate(saved, saved.getOwnerId()));
    }
}
