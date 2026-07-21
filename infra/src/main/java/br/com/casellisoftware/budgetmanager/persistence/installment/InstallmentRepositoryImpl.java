package br.com.casellisoftware.budgetmanager.persistence.installment;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.installment.mappers.InstallmentPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InstallmentRepositoryImpl implements InstallmentRepository {

    private final InstallmentMongoRepository installmentMongoRepository;
    private final InstallmentPersistenceMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public Installment save(Installment installment) {
        // Fetch existing version so @Version-based optimistic locking works on updates.
        Long version = installmentMongoRepository.findById(installment.getId())
                .map(InstallmentDocument::getVersion)
                .orElse(null);
        InstallmentDocument saved = installmentMongoRepository.save(
                mapper.toDocument(installment, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Installment> findById(String id) {
        return installmentMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Installment> findById(String id, String ownerId) {
        return installmentMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public Map<String, Installment> findAllByIds(Collection<String> ids, String ownerId) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return installmentMongoRepository.findAllByIdInAndOwnerId(ids, ownerId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(Installment::getId, installment -> installment));
    }

    @Override
    public boolean existsById(String id) {
        return installmentMongoRepository.existsById(id);
    }

    @Override
    public boolean existsById(String id, String ownerId) {
        return installmentMongoRepository.findByIdAndOwnerId(id, ownerId).isPresent();
    }

    @Override
    public List<Installment> findActiveAffecting(YearMonth walletMonth) {
        return installmentMongoRepository.findActiveAffecting(walletMonth).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Installment> findActiveAffecting(YearMonth walletMonth, String ownerId) {
        return installmentMongoRepository.findActiveAffecting(walletMonth, ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Installment> findActiveAffectingAny(Collection<YearMonth> walletMonths) {
        if (walletMonths == null || walletMonths.isEmpty()) {
            return List.of();
        }

        Criteria[] perMonthCriteria = walletMonths.stream()
                .distinct()
                .map(walletMonth -> perMonthAffectingCriteria(walletMonth.toString()))
                .toArray(Criteria[]::new);

        Query query = new Query(new Criteria().orOperator(perMonthCriteria));
        return mongoTemplate.find(query, InstallmentDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Installment> findActiveAffectingAny(Collection<YearMonth> walletMonths, String ownerId) {
        if (walletMonths == null || walletMonths.isEmpty()) {
            return List.of();
        }

        Criteria[] perMonthCriteria = walletMonths.stream()
                .distinct()
                .map(walletMonth -> perMonthAffectingCriteria(walletMonth.toString()))
                .toArray(Criteria[]::new);

        Query query = new Query(new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                new Criteria().orOperator(perMonthCriteria)));
        return mongoTemplate.find(query, InstallmentDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * Per-month criteria for installments affecting a wallet month.
     *
     * <p>Two subtleties baked in:
     * <ul>
     *   <li>{@code walletMonth} must be passed as its {@code YYYY-MM} String form.
     *   Passing a {@link YearMonth} instance would be BSON-encoded as a sub-document
     *   {year, month}, which never matches the stored string and silently returns
     *   an empty result set.</li>
     *   <li>Source-month inclusiveness depends on the installment's source:
     *   standalone ({@code sourceWalletId == null}) is inclusive (the first parcel
     *   charges in {@code sourceEffectiveMonth} itself); from-expense
     *   ({@code sourceWalletId != null}) is exclusive (the purchase already
     *   counted in the source wallet's month).</li>
     * </ul>
     */
    private static Criteria perMonthAffectingCriteria(String walletMonthAsString) {
        Criteria standalone = new Criteria().andOperator(
                Criteria.where("sourceWalletId").is(null),
                Criteria.where("sourceEffectiveMonth").lte(walletMonthAsString)
        );
        Criteria fromExpense = new Criteria().andOperator(
                Criteria.where("sourceWalletId").ne(null),
                Criteria.where("sourceEffectiveMonth").lt(walletMonthAsString)
        );
        return new Criteria().andOperator(
                Criteria.where("deleted").is(false),
                new Criteria().orOperator(standalone, fromExpense),
                Criteria.where("lastInstallmentDate").gte(walletMonthAsString)
        );
    }

    @Override
    public List<Installment> findBySourceWalletIdAndNotDeleted(String sourceWalletId) {
        return installmentMongoRepository.findBySourceWalletIdAndNotDeleted(sourceWalletId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Installment> findBySourceWalletIdAndNotDeleted(String sourceWalletId, String ownerId) {
        return installmentMongoRepository.findBySourceWalletIdAndNotDeleted(sourceWalletId, ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<String> findIdsByCreditCardId(String creditCardId) {
        return installmentMongoRepository.findIdsByCreditCardId(creditCardId).stream()
                .map(InstallmentDocument::getId)
                .toList();
    }

    @Override
    public List<String> findIdsByCreditCardId(String creditCardId, String ownerId) {
        return installmentMongoRepository.findIdsByCreditCardId(creditCardId, ownerId).stream()
                .map(InstallmentDocument::getId)
                .toList();
    }

    @Override
    public List<String> findIdsByCreditCardIdAndNotDeleted(String creditCardId) {
        return installmentMongoRepository.findIdsByCreditCardIdAndNotDeleted(creditCardId).stream()
                .map(InstallmentDocument::getId)
                .toList();
    }

    @Override
    public List<String> findIdsByCreditCardIdAndNotDeleted(String creditCardId, String ownerId) {
        return installmentMongoRepository.findIdsByCreditCardIdAndNotDeleted(creditCardId, ownerId).stream()
                .map(InstallmentDocument::getId)
                .toList();
    }

    @Override
    public PageResult<Installment> findByWalletContext(String walletId,
                                                       YearMonth effectiveMonth,
                                                       String creditCardId,
                                                       InstallmentSortOrder sortOrder,
                                                       int page,
                                                       int size,
                                                       String ownerId) {
        Criteria baseCriteria = buildWalletContextCriteria(walletId, effectiveMonth, creditCardId, ownerId);
        Sort sort = buildWalletContextSort(sortOrder);

        Query countQuery = new Query(baseCriteria);
        long total = mongoTemplate.count(countQuery, InstallmentDocument.class);

        Query pageQuery = new Query(baseCriteria)
                .with(sort)
                .skip((long) page * size)
                .limit(size);

        List<Installment> installments = mongoTemplate.find(pageQuery, InstallmentDocument.class)
                .stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(installments, page, size, total, totalPages);
    }

    @Override
    public List<Installment> findByWalletContext(String walletId,
                                                 YearMonth effectiveMonth,
                                                 String creditCardId,
                                                 InstallmentSortOrder sortOrder,
                                                 String ownerId) {
        Criteria baseCriteria = buildWalletContextCriteria(walletId, effectiveMonth, creditCardId, ownerId);
        Sort sort = buildWalletContextSort(sortOrder);

        Query query = new Query(baseCriteria).with(sort);
        return mongoTemplate.find(query, InstallmentDocument.class).stream()
                .map(mapper::toDomain)
                .toList();
    }

    private Criteria buildWalletContextCriteria(String walletId,
                                                YearMonth effectiveMonth,
                                                String creditCardId,
                                                String ownerId) {
        // Branch 1: installment was sourced from this wallet
        Criteria sourceWalletBranch = Criteria.where("sourceWalletId").is(walletId);

        // Branch 2: active installment that affects the wallet's effective month.
        // YearMonth must be serialized to its String form (YYYY-MM); passing the
        // YearMonth instance directly causes Spring Data to BSON-encode it as a
        // sub-document {year, month}, which never matches the stored string and
        // silently returns an empty result set.
        //
        // Semantics differ by source:
        //   - Standalone (sourceWalletId == null): the first parcel is charged in
        //     `sourceEffectiveMonth` itself, so the source month is inclusive
        //     (`sourceEffectiveMonth <= walletMonth`).
        //   - From-expense (sourceWalletId != null): the purchase was already
        //     counted in the source wallet's month, so the first parcel only
        //     materializes the month after (`sourceEffectiveMonth < walletMonth`).
        String effectiveMonthAsString = effectiveMonth.toString();
        Criteria standaloneAffecting = new Criteria().andOperator(
                Criteria.where("sourceWalletId").is(null),
                Criteria.where("sourceEffectiveMonth").lte(effectiveMonthAsString)
        );
        Criteria fromExpenseAffecting = new Criteria().andOperator(
                Criteria.where("sourceWalletId").ne(null),
                Criteria.where("sourceEffectiveMonth").lt(effectiveMonthAsString)
        );
        Criteria activeAffectingBranch = new Criteria().andOperator(
                new Criteria().orOperator(standaloneAffecting, fromExpenseAffecting),
                Criteria.where("lastInstallmentDate").gte(effectiveMonthAsString)
        );

        Criteria baseCriteria = new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("deleted").is(false),
                new Criteria().orOperator(sourceWalletBranch, activeAffectingBranch)
        );

        if (creditCardId != null && !creditCardId.isBlank()) {
            return new Criteria().andOperator(baseCriteria, Criteria.where("creditCardId").is(creditCardId));
        }

        return baseCriteria;
    }

    @Override
    public PageResult<Installment> findFinishedByWalletContext(YearMonth effectiveMonth,
                                                               String creditCardId,
                                                               InstallmentSortOrder sortOrder,
                                                               int page,
                                                               int size,
                                                               String ownerId) {
        Criteria baseCriteria = buildFinishedCriteria(effectiveMonth, creditCardId, ownerId);
        Sort sort = buildWalletContextSort(sortOrder);

        Query countQuery = new Query(baseCriteria);
        long total = mongoTemplate.count(countQuery, InstallmentDocument.class);

        Query pageQuery = new Query(baseCriteria)
                .with(sort)
                .skip((long) page * size)
                .limit(size);

        List<Installment> installments = mongoTemplate.find(pageQuery, InstallmentDocument.class)
                .stream()
                .map(mapper::toDomain)
                .toList();

        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResult<>(installments, page, size, total, totalPages);
    }

    private Criteria buildFinishedCriteria(YearMonth effectiveMonth,
                                           String creditCardId,
                                           String ownerId) {
        // YearMonth must be serialized to its String form (YYYY-MM); passing the
        // YearMonth instance directly causes Spring Data to BSON-encode it as a
        // sub-document {year, month}, which never matches the stored string and
        // silently returns an empty result set.
        String effectiveMonthAsString = effectiveMonth.toString();

        Criteria baseCriteria = new Criteria().andOperator(
                Criteria.where("ownerId").is(ownerId),
                Criteria.where("deleted").is(false),
                Criteria.where("lastInstallmentDate").lt(effectiveMonthAsString)
        );

        if (creditCardId != null && !creditCardId.isBlank()) {
            return new Criteria().andOperator(baseCriteria, Criteria.where("creditCardId").is(creditCardId));
        }

        return baseCriteria;
    }

    private Sort buildWalletContextSort(InstallmentSortOrder sortOrder) {
        return sortOrder == InstallmentSortOrder.ENDING_LATE
                ? Sort.by(Sort.Direction.DESC, "lastInstallmentDate")
                : Sort.by(Sort.Direction.ASC, "lastInstallmentDate");
    }

    @Override
    public PageResult<Installment> findAll(int page, int size) {
        Page<InstallmentDocument> documentPage = installmentMongoRepository.findAll(PageRequest.of(page, size));
        List<Installment> installments = documentPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();
        return new PageResult<>(
                installments,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public PageResult<Installment> findAll(int page, int size, String ownerId) {
        Page<InstallmentDocument> documentPage = installmentMongoRepository.findAllByOwnerId(ownerId, PageRequest.of(page, size));
        List<Installment> installments = documentPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();
        return new PageResult<>(
                installments,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }
}
