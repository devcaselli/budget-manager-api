package br.com.casellisoftware.budgetmanager.persistence.payer;

import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.persistence.payer.mappers.PayerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PayerRepositoryImpl implements PayerRepository {

    private final PayerMongoRepository payerMongoRepository;
    private final PayerPersistenceMapper mapper;

    @Override
    public Payer save(Payer payer) {
        Long version = payerMongoRepository.findById(payer.getId())
                .map(PayerDocument::getVersion)
                .orElse(null);
        PayerDocument saved = payerMongoRepository.save(mapper.toDocument(payer, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Payer> findById(String id) {
        return payerMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payer> findById(String id, String ownerId) {
        return payerMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return payerMongoRepository.existsById(id);
    }

    @Override
    public boolean existsById(String id, String ownerId) {
        return payerMongoRepository.findByIdAndOwnerId(id, ownerId).isPresent();
    }

    @Override
    public List<Payer> findAll(String ownerId) {
        return payerMongoRepository.findAllByOwnerIdAndDeletedFalse(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payer> findAllStanding(String ownerId) {
        return payerMongoRepository.findAllByOwnerIdAndTypeAndDeletedFalse(ownerId, PayerType.STANDING.name())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payer> findAllByWalletId(String walletId, String ownerId) {
        return payerMongoRepository.findAllByOwnerIdAndWalletIdAndDeletedFalse(ownerId, walletId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Payer> findAllByIdsIn(Collection<String> ids, String ownerId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return payerMongoRepository.findAllByIdInAndOwnerIdAndDeletedFalse(ids, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        findById(id).ifPresent(payer -> save(payer.delete()));
    }

    @Override
    public void deleteById(String id, String ownerId) {
        findById(id, ownerId).ifPresent(payer -> save(payer.delete()));
    }

    @Override
    public void deleteAllByWalletId(String walletId, String ownerId) {
        payerMongoRepository.findAllByWalletIdAndOwnerIdAndDeletedFalse(walletId, ownerId)
                .forEach(document -> payerMongoRepository.save(mapper.toDocument(mapper.toDomain(document).delete(), document.getVersion())));
    }
}
