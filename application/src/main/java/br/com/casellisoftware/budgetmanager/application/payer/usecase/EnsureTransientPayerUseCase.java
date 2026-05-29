package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class EnsureTransientPayerUseCase {

    private final PayerRepository payerRepository;
    private final Clock clock;

    public EnsureTransientPayerUseCase(PayerRepository payerRepository, Clock clock) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public String execute(TransientPayerSpec spec, String walletId, String ownerId) {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        if (spec.payerId() != null) {
            return payerRepository.findById(spec.payerId(), ownerId)
                    .map(Payer::getId)
                    .orElseThrow(() -> new PayerNotFoundException(spec.payerId()));
        }

        if (spec.name() == null || spec.name().isBlank()) {
            throw new IllegalArgumentException("name must not be blank when payerId is not provided");
        }

        List<Payer> walletPayers = payerRepository.findAllByWalletId(walletId, ownerId);
        for (Payer payer : walletPayers) {
            if (payer.getType() == PayerType.TRANSIENT
                    && payer.getName().equalsIgnoreCase(spec.name().trim())) {
                return payer.getId();
            }
        }

        LocalDate paymentDate = spec.paymentDate() == null ? LocalDate.now(clock) : spec.paymentDate();
        Payer created = payerRepository.save(Payer.create(
                spec.name().trim(),
                PayerType.TRANSIENT,
                walletId,
                null,
                paymentDate,
                ownerId
        ));
        return created.getId();
    }
}
