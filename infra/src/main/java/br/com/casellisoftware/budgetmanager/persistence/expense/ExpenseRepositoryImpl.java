package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardResult;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data. No
 * business decisions, no domain exceptions, no input validation beyond what
 * the {@link Expense} entity already guarantees.
 */
@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final ExpenseMongoRepository expenseMongoRepository;
    private final ExpensePersistenceMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Expense save(Expense expense) {
        Long version = expenseMongoRepository.findById(expense.getId())
                .map(ExpenseDocument::getVersion)
                .orElse(null);
        ExpenseDocument saved = expenseMongoRepository.save(mapper.toDocument(expense, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Expense> findById(String id) {
        return this.expenseMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Expense> findById(String id, String ownerId) {
        return this.expenseMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String id) {
        return this.expenseMongoRepository.existsById(id);
    }

    @Override
    public boolean existsById(String id, String ownerId) {
        return this.expenseMongoRepository.findByIdAndOwnerId(id, ownerId).isPresent();
    }

    @Override
    public boolean existsAnyByCreditCardId(String creditCardId) {
        return expenseMongoRepository.existsByCreditCardId(creditCardId);
    }

    @Override
    public boolean existsAnyByCreditCardId(String creditCardId, String ownerId) {
        return expenseMongoRepository.existsByCreditCardIdAndOwnerId(creditCardId, ownerId);
    }

    @Override
    public PageResult<Expense> findByWalletId(String walletId, int page, int size, boolean unhidden) {
        Query query = new Query(Criteria.where("walletId").is(walletId));
        if (!unhidden) {
            query.addCriteria(Criteria.where("hidden").ne(true));
        }

        long totalElements = mongoTemplate.count(query, ExpenseDocument.class);
        Query pagedQuery = Query.of(query).with(PageRequest.of(page, size));

        List<Expense> expenses = mongoTemplate.find(pagedQuery, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return new PageResult<>(
                expenses,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    @Override
    public PageResult<Expense> findByWalletId(String walletId, int page, int size, boolean unhidden, String ownerId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("walletId").is(walletId)));
        if (!unhidden) {
            query.addCriteria(Criteria.where("hidden").ne(true));
        }

        long totalElements = mongoTemplate.count(query, ExpenseDocument.class);
        Query pagedQuery = Query.of(query).with(PageRequest.of(page, size));

        List<Expense> expenses = mongoTemplate.find(pagedQuery, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        return new PageResult<>(expenses, page, size, totalElements, totalPages);
    }

    @Override
    public List<Expense> findByOwnerIdAndPurchaseDateGreaterThanOrEqual(String ownerId, LocalDate startDate) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("purchaseDate").gte(startDate),
                Criteria.where("hidden").ne(true)));
        query.with(Sort.by(Sort.Direction.ASC, "purchaseDate", "id"));

        return mongoTemplate.find(query, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Expense> findAllByOwnerId(String ownerId) {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("hidden").ne(true)));
        query.with(Sort.by(Sort.Direction.ASC, "purchaseDate", "id"));

        return mongoTemplate.find(query, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ExpenseByCreditCardResult findByCreditCardId(String creditCardId,
                                                        ExpenseByCreditCardFilter filter,
                                                        int page,
                                                        int size) {
        Criteria criteria = buildCreditCardCriteria(creditCardId, filter);
        Query baseQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(baseQuery, ExpenseDocument.class);

        Query pageQuery = Query.of(baseQuery).with(PageRequest.of(page, size));
        List<Expense> expenses = mongoTemplate.find(pageQuery, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        BigDecimal totalCost = aggregateTotalCost(criteria);

        return new ExpenseByCreditCardResult(
                new PageResult<>(expenses, page, size, totalElements, totalPages),
                totalCost
        );
    }

    @Override
    public ExpenseByCreditCardResult findByCreditCardId(String creditCardId,
                                                        ExpenseByCreditCardFilter filter,
                                                        int page,
                                                        int size,
                                                        String ownerId) {
        Criteria criteria = buildCreditCardCriteria(creditCardId, filter, ownerId);
        Query baseQuery = new Query(criteria);
        long totalElements = mongoTemplate.count(baseQuery, ExpenseDocument.class);

        Query pageQuery = Query.of(baseQuery).with(PageRequest.of(page, size));
        List<Expense> expenses = mongoTemplate.find(pageQuery, ExpenseDocument.class).stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        BigDecimal totalCost = aggregateTotalCost(criteria);

        return new ExpenseByCreditCardResult(
                new PageResult<>(expenses, page, size, totalElements, totalPages),
                totalCost
        );
    }

    @Override
    public List<String> findIdsByCreditCardId(String creditCardId) {
        return expenseMongoRepository.findIdsByCreditCardId(creditCardId)
                .stream()
                .map(ExpenseDocument::getId)
                .toList();
    }

    @Override
    public List<String> findIdsByCreditCardId(String creditCardId, String ownerId) {
        return expenseMongoRepository.findIdsByCreditCardId(creditCardId, ownerId)
                .stream()
                .map(ExpenseDocument::getId)
                .toList();
    }

    @Override
    public Optional<Expense> findByInstallmentId(String installmentId) {
        return expenseMongoRepository.findByInstallmentId(installmentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Expense> findByInstallmentId(String installmentId, String ownerId) {
        return expenseMongoRepository.findByInstallmentIdAndOwnerId(installmentId, ownerId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(String id, String ownerId) {
        long deletedCount = this.expenseMongoRepository.deleteByIdAndOwnerId(id, ownerId);
        if (deletedCount == 0) {
            throw new ExpenseNotFoundException(id);
        }
    }

    @Override
    public Optional<Expense> findBySourcePendingId(String sourcePendingId, String ownerId) {
        return expenseMongoRepository.findBySourcePendingIdAndOwnerId(sourcePendingId, ownerId)
                .map(mapper::toDomain);
    }

    private Criteria buildCreditCardCriteria(String creditCardId, ExpenseByCreditCardFilter filter) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("creditCardId").is(creditCardId));
        criteria.add(Criteria.where("hidden").ne(true));
        // Standard installments generate a per-month child expense linked to
        // the installment. That child must NOT surface on the credit-card view:
        // the user sees only the Installment object itself there. Pure
        // expenses (`installmentId == null`) remain visible as before.
        criteria.add(Criteria.where("installmentId").is(null));

        if (filter.walletIds() != null) {
            criteria.add(Criteria.where("walletId").in(filter.walletIds()));
        }

        if (filter.name() != null) {
            criteria.add(Criteria.where("name").regex(Pattern.compile(Pattern.quote(filter.name()), Pattern.CASE_INSENSITIVE)));
        }

        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }

    private Criteria buildCreditCardCriteria(String creditCardId, ExpenseByCreditCardFilter filter, String ownerId) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("ownerId").is(ownerId));
        criteria.add(Criteria.where("creditCardId").is(creditCardId));
        criteria.add(Criteria.where("hidden").ne(true));
        criteria.add(Criteria.where("installmentId").is(null));

        if (filter.walletIds() != null) {
            criteria.add(Criteria.where("walletId").in(filter.walletIds()));
        }

        if (filter.name() != null) {
            criteria.add(Criteria.where("name").regex(Pattern.compile(Pattern.quote(filter.name()), Pattern.CASE_INSENSITIVE)));
        }

        return new Criteria().andOperator(criteria.toArray(Criteria[]::new));
    }

    private BigDecimal aggregateTotalCost(Criteria criteria) {
        AggregationResults<Document> result = mongoTemplate.aggregate(
                Aggregation.newAggregation(
                        Aggregation.match(criteria),
                        Aggregation.group().sum("cost").as("totalCost")
                ),
                mongoTemplate.getCollectionName(ExpenseDocument.class),
                Document.class
        );

        Document document = result.getUniqueMappedResult();
        if (document == null || document.get("totalCost") == null) {
            return BigDecimal.ZERO;
        }

        Object totalCost = document.get("totalCost");
        if (totalCost instanceof BigDecimal value) {
            return value;
        }

        return new BigDecimal(totalCost.toString());
    }
}
