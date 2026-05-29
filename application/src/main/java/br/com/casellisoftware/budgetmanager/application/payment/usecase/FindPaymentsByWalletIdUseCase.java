package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindPaymentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.util.List;

public class FindPaymentsByWalletIdUseCase implements FindPaymentsByWalletIdBoundary {

    private final PaymentRepository paymentRepository;
    private final FindWalletByIdBoundary findWalletByIdBoundary;

    public FindPaymentsByWalletIdUseCase(PaymentRepository paymentRepository,
                                         FindWalletByIdBoundary findWalletByIdBoundary) {
        this.paymentRepository = paymentRepository;
        this.findWalletByIdBoundary = findWalletByIdBoundary;
    }

    @Override
    public PageResult<PaymentOutput> execute(String walletId, int page, int size, String ownerId) {
        findWalletByIdBoundary.findById(walletId, ownerId);

        PageResult<Payment> result = paymentRepository.findByWalletId(walletId, page, size, ownerId);
        List<PaymentOutput> outputs = result.content()
                .stream()
                .map(PaymentOutputAssembler::from)
                .toList();

        return new PageResult<>(
                outputs,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
