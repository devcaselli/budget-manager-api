package br.com.casellisoftware.budgetmanager.persistence.creditcard;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.mappers.CreditCardPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CreditCardRepositoryImpl implements CreditCardRepository {

    private final CreditCardMongoRepository creditCardMongoRepository;
    private final CreditCardPersistenceMapper mapper;

    @Override
    public CreditCard save(CreditCard creditCard) {
        Long version = creditCardMongoRepository.findById(creditCard.getId())
                .map(CreditCardDocument::getVersion)
                .orElse(null);
        CreditCardDocument saved = creditCardMongoRepository.save(
                mapper.toDocument(creditCard, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CreditCard> findById(String id) {
        return creditCardMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CreditCard> findById(String id, String ownerId) {
        return creditCardMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return creditCardMongoRepository.existsById(id);
    }

    @Override
    public boolean existsById(String id, String ownerId) {
        return creditCardMongoRepository.findByIdAndOwnerId(id, ownerId).isPresent();
    }

    @Override
    public PageResult<CreditCard> findAll(int page, int size) {
        Page<CreditCardDocument> documentPage = creditCardMongoRepository.findAll(
                PageRequest.of(page, size));

        List<CreditCard> cards = documentPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                cards,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public PageResult<CreditCard> findAll(int page, int size, String ownerId) {
        Page<CreditCardDocument> documentPage = creditCardMongoRepository.findAllByOwnerId(
                ownerId, PageRequest.of(page, size));

        List<CreditCard> cards = documentPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                cards,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public void deleteById(String id) {
        creditCardMongoRepository.deleteById(id);
    }

    @Override
    public Optional<CreditCard> findByNormalizedLabel(String normalizedLabel, String ownerId) {
        return creditCardMongoRepository.findByNormalizedLabelsContainingAndOwnerId(normalizedLabel, ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CreditCard> findByName(String name, String ownerId) {
        return creditCardMongoRepository.findByNameAndOwnerId(name, ownerId)
                .map(mapper::toDomain);
    }
}
