package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindPayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;

import java.util.Objects;

public class FindPayerByIdUseCase implements FindPayerByIdBoundary {

    private final PayerRepository payerRepository;
    private final PayerAmountDueCalculator calculator;

    public FindPayerByIdUseCase(PayerRepository payerRepository, PayerAmountDueCalculator calculator) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
    }

    @Override
    public PayerOutput findById(String id, String ownerId) {
        Payer payer = payerRepository.findById(id, ownerId)
                .orElseThrow(() -> new PayerNotFoundException(id));
        return PayerOutputAssembler.from(payer, calculator.calculate(payer, ownerId));
    }
}
