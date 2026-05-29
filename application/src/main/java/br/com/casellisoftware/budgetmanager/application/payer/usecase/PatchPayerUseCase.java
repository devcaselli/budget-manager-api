package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerLifecycleChangeNotAllowedException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PatchPayerUseCase implements PatchPayerBoundary {

    private final PayerRepository payerRepository;
    private final PayerAmountDueCalculator calculator;
    private final ShareRepository shareRepository;

    public PatchPayerUseCase(PayerRepository payerRepository, PayerAmountDueCalculator calculator) {
        this(payerRepository, calculator, NoOpShareRepository.INSTANCE);
    }

    public PatchPayerUseCase(PayerRepository payerRepository,
                             PayerAmountDueCalculator calculator,
                             ShareRepository shareRepository) {
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
        this.calculator = Objects.requireNonNull(calculator, "calculator must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public PayerOutput execute(String id, PayerPatchInput patch, String ownerId) {
        Objects.requireNonNull(patch, "patch must not be null");
        Payer existing = payerRepository.findById(id, ownerId)
                .orElseThrow(() -> new PayerNotFoundException(id));
        if (patch.type().isPresent()
                && patch.type().get() != existing.getType()
                && shareRepository.existsByPayerId(existing.getId(), ownerId)) {
            throw new PayerLifecycleChangeNotAllowedException(existing.getId());
        }
        Payer saved = payerRepository.save(existing.patch(patch.toPatch()));
        return PayerOutputAssembler.from(saved, calculator.calculate(saved, ownerId));
    }

    private static final class NoOpShareRepository implements ShareRepository {
        private static final NoOpShareRepository INSTANCE = new NoOpShareRepository();

        @Override
        public Share save(Share share) {
            throw new UnsupportedOperationException("share persistence is not configured");
        }

        @Override
        public Optional<Share> findById(String id, String ownerId) {
            return Optional.empty();
        }

        @Override
        public Optional<Share> findActiveBySourceId(ShareSourceType type, String sourceId, String ownerId) {
            return Optional.empty();
        }

        @Override
        public Map<String, Share> findActiveBySourceIds(ShareSourceType type,
                                                        Collection<String> sourceIds,
                                                        String ownerId) {
            return Map.of();
        }

        @Override
        public List<Share> findAllByOwner(String ownerId) {
            return List.of();
        }

        @Override
        public boolean existsActiveBySourceId(ShareSourceType type, String sourceId, String ownerId) {
            return false;
        }

        @Override
        public boolean existsByPayerId(String payerId, String ownerId) {
            return false;
        }

        @Override
        public List<Share> findActiveByPayerId(String payerId, String ownerId) {
            return List.of();
        }
    }
}
