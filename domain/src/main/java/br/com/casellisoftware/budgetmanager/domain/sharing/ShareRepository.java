package br.com.casellisoftware.budgetmanager.domain.sharing;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ShareRepository {

    Share save(Share share);

    Optional<Share> findById(String id, String ownerId);

    Optional<Share> findActiveBySourceId(ShareSourceType type, String sourceId, String ownerId);

    /**
     * Batch lookup of active shares for the given source ids. Returns a map keyed by
     * {@link Share#getSourceId()} so callers can resolve each source in O(1) without
     * issuing N queries.
     *
     * @param type     the source type to filter (e.g. INSTALLMENT, SUBSCRIPTION)
     * @param sourceIds the source ids to fetch; empty collection returns empty map
     * @param ownerId   the owner scope
     * @return map of sourceId -> active Share; sources without active share are absent
     */
    Map<String, Share> findActiveBySourceIds(ShareSourceType type,
                                             Collection<String> sourceIds,
                                             String ownerId);

    List<Share> findAllByOwner(String ownerId);

    boolean existsActiveBySourceId(ShareSourceType type, String sourceId, String ownerId);

    boolean existsByPayerId(String payerId, String ownerId);

    /**
     * Active shares whose quotas reference the given payer.
     */
    List<Share> findActiveByPayerId(String payerId, String ownerId);
}
