package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyConnectionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;

import java.util.List;
import java.util.Objects;

/**
 * Lists the Pluggy connections registered by an owner.
 *
 * @implNote Time complexity: O(c), Space complexity: O(c) where c = number of connections
 *           for the owner (single indexed query).
 */
public class FindPluggyConnectionsUseCase implements FindPluggyConnectionsBoundary {

    private final PluggyConnectionRepository pluggyConnectionRepository;

    public FindPluggyConnectionsUseCase(PluggyConnectionRepository pluggyConnectionRepository) {
        this.pluggyConnectionRepository = Objects.requireNonNull(pluggyConnectionRepository, "pluggyConnectionRepository must not be null");
    }

    @Override
    public List<PluggyConnectionOutput> execute(String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        return pluggyConnectionRepository.findByOwnerId(ownerId).stream()
                .map(RegisterPluggyItemUseCase::toOutput)
                .toList();
    }
}
