package br.com.casellisoftware.budgetmanager.persistence.wallet;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.persistence.wallet.mappers.WalletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WalletRepositoryImpl implements WalletRepository {

    private final WalletMongoRepository walletMongoRepository;
    private final WalletPersistenceMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<Wallet> findById(String id) {
        return this.walletMongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findById(String id, String ownerId) {
        return this.walletMongoRepository.findByIdAndOwnerId(id, ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Wallet> findAll() {
        return this.walletMongoRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Wallet> findAll(String ownerId) {
        return this.walletMongoRepository.findAllByOwnerId(ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Wallet save(Wallet wallet) {
        Long version = walletMongoRepository.findById(wallet.getId())
                .map(WalletDocument::getVersion)
                .orElse(null);
        WalletDocument document = mapper.toDocument(wallet, version);
        document = this.walletMongoRepository.save(document);
        return  mapper.toDomain(document);
    }

    @Override
    public List<String> findIdsByEffectiveMonth(YearMonth effectiveMonth) {
        Query query = new Query(Criteria.where("effectiveMonth").is(effectiveMonth.toString()));
        query.fields().include("_id");

        return mongoTemplate.find(query, WalletDocument.class).stream()
                .map(WalletDocument::getId)
                .toList();
    }

    @Override
    public List<String> findIdsByEffectiveMonth(YearMonth effectiveMonth, String ownerId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("effectiveMonth").is(effectiveMonth.toString())));
        query.fields().include("_id");

        return mongoTemplate.find(query, WalletDocument.class).stream()
                .map(WalletDocument::getId)
                .toList();
    }

    @Override
    public Optional<Wallet> findCurrentProductionOpen(YearMonth effectiveMonth, LocalDate today) {
        return this.walletMongoRepository.findCurrentProductionOpen(effectiveMonth.toString(), today)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Wallet> findCurrentProductionOpen(YearMonth effectiveMonth, LocalDate today, String ownerId) {
        return this.walletMongoRepository.findCurrentProductionOpen(effectiveMonth.toString(), today, ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsOpenProductionFor(YearMonth effectiveMonth, LocalDate today, String excludeId) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("effectiveMonth").is(effectiveMonth.toString()));
        criteria.add(Criteria.where("state").is(WalletState.PRODUCTION.name()));
        criteria.add(Criteria.where("isClosed").ne(true));
        criteria.add(new Criteria().orOperator(
                Criteria.where("closedDate").is(null),
                Criteria.where("closedDate").gt(today)));
        if (excludeId != null) {
            criteria.add(Criteria.where("_id").ne(excludeId));
        }

        Query query = new Query(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        return mongoTemplate.exists(query, WalletDocument.class);
    }

    @Override
    public boolean existsOpenProductionFor(YearMonth effectiveMonth, LocalDate today, String excludeId, String ownerId) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("ownerId").is(ownerId));
        criteria.add(Criteria.where("effectiveMonth").is(effectiveMonth.toString()));
        criteria.add(Criteria.where("state").is(WalletState.PRODUCTION.name()));
        criteria.add(Criteria.where("isClosed").ne(true));
        criteria.add(new Criteria().orOperator(
                Criteria.where("closedDate").is(null),
                Criteria.where("closedDate").gt(today)));
        if (excludeId != null) {
            criteria.add(Criteria.where("_id").ne(excludeId));
        }

        Query query = new Query(new Criteria().andOperator(criteria.toArray(Criteria[]::new)));
        return mongoTemplate.exists(query, WalletDocument.class);
    }

    @Override
    public List<Wallet> findAllProductionByOwnerId(String ownerId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("state").is(WalletState.PRODUCTION.name())
        ));
        return mongoTemplate.find(query, WalletDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
