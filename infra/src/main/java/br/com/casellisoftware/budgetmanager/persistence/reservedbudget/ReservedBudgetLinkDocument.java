package br.com.casellisoftware.budgetmanager.persistence.reservedbudget;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

/**
 * Embedded document for a {@link br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink}
 * stored inside {@link ReservedBudgetDocument#getLinks()}.
 *
 * <p>A multikey compound index on {@code (links.sourceType, links.sourceId)} is declared on
 * the parent document via {@code @CompoundIndex} to support efficient cardinality lookups
 * ({@code findByLinkedSource}).</p>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservedBudgetLinkDocument {

    /** String representation of {@link br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType}. */
    private String sourceType;

    private String sourceId;

    /** The first month from which this link is applicable (inclusive). */
    private YearMonth fromMonth;
}
