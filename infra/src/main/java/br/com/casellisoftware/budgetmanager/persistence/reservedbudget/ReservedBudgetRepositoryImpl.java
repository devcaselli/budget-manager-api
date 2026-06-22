package br.com.casellisoftware.budgetmanager.persistence.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.mappers.ReservedBudgetPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data. Logical delete is
 * handled by the use case via {@code save} of a deleted instance; queries filter out
 * {@code deleted == true}.
 */
@Repository
@RequiredArgsConstructor
public class ReservedBudgetRepositoryImpl implements ReservedBudgetRepository {

    private final ReservedBudgetMongoRepository reservedBudgetMongoRepository;
    private final ReservedBudgetPersistenceMapper mapper;

    @Override
    public ReservedBudget save(ReservedBudget reservedBudget) {
        Long version = reservedBudgetMongoRepository.findById(reservedBudget.getId())
                .map(ReservedBudgetDocument::getVersion)
                .orElse(null);
        ReservedBudgetDocument saved = reservedBudgetMongoRepository.save(mapper.toDocument(reservedBudget, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ReservedBudget> findById(String id) {
        return reservedBudgetMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ReservedBudget> findById(String id, String ownerId) {
        return reservedBudgetMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<ReservedBudget> findActiveFor(YearMonth month, String ownerId) {
        String monthAsString = Objects.requireNonNull(month, "month must not be null").toString();
        return reservedBudgetMongoRepository.findActiveFor(monthAsString, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ReservedBudget> findActiveForAny(List<YearMonth> months, String ownerId) {
        Objects.requireNonNull(months, "months must not be null");
        if (months.isEmpty()) {
            return List.of();
        }
        // A reserved budget has no end, so "active for any of these months" reduces to
        // startMonth <= max(months). Callers filter per-month via ReservedBudget.isApplicable.
        YearMonth maxMonth = months.stream().max(YearMonth::compareTo).orElseThrow();
        return findActiveFor(maxMonth, ownerId);
    }

    @Override
    public Optional<ReservedBudget> findByLinkedSource(ReservedBudgetLinkSourceType sourceType,
                                                       String sourceId,
                                                       String ownerId) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        return reservedBudgetMongoRepository
                .findByLinkedSource(sourceType.name(), sourceId, ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<ReservedBudget> findAll(int page, int size, String ownerId) {
        Page<ReservedBudgetDocument> documentPage = reservedBudgetMongoRepository.findAllByOwnerId(ownerId, PageRequest.of(page, size));
        List<ReservedBudget> reservedBudgets = documentPage.getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                reservedBudgets,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }
}
