package br.com.casellisoftware.budgetmanager.domain.shared;

import java.util.List;
import java.util.Objects;

/**
 * Framework-agnostic representation of a paginated result set.
 *
 * <p>Lives in the domain so that repository ports can express pagination
 * without pulling in Spring's {@code Page}/{@code Pageable}. Adapters in the
 * infra layer convert from/to the framework type.</p>
 *
 * @param content        the slice of elements for the requested page
 * @param page           zero-based page index
 * @param size           requested page size
 * @param totalElements  total number of elements across all pages
 * @param totalPages     total number of pages
 */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        Objects.requireNonNull(content, "content must not be null");
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1) throw new IllegalArgumentException("size must be at least 1");
        if (totalElements < 0) throw new IllegalArgumentException("totalElements must not be negative");
        if (totalPages < 0) throw new IllegalArgumentException("totalPages must not be negative");
        content = List.copyOf(content);
    }
}
