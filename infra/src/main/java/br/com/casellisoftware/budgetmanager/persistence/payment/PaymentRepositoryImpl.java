package br.com.casellisoftware.budgetmanager.persistence.payment;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.payment.mappers.PaymentPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data. No
 * business decisions, no domain exceptions, no input validation beyond what
 * the {@link Payment} entity already guarantees.
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentMongoRepository paymentMongoRepository;
    private final PaymentPersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        PaymentDocument saved = this.paymentMongoRepository.save(mapper.toDocument(payment));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(String id) {
        return this.paymentMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Payment> findByWalletId(String walletId, int page, int size) {
        Page<PaymentDocument> documentPage = this.paymentMongoRepository
                .findByWalletId(walletId, PageRequest.of(page, size));

        List<Payment> payments = documentPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                payments,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public void deleteById(String id) {
        this.paymentMongoRepository.deleteById(id);
    }

    @Override
    public List<Payment> findAllByExpenseId(String expenseId) {
        return this.paymentMongoRepository.findAllByExpenseId(expenseId)
                .stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteAllById(List<String> ids) {
        this.paymentMongoRepository.deleteAllById(ids);
    }

    @Override
    public boolean existsByBulletId(String bulletId) {
        return this.paymentMongoRepository.existsByBulletId(bulletId);
    }
}
