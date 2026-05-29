package br.com.casellisoftware.budgetmanager.domain.flag;

/**
 * Catalog of feature flags used by the domain and application layers.
 *
 * <p>Precedence rule:
 * <ol>
 *   <li>For read/update flows, the persisted aggregate flag is the source of truth.</li>
 *   <li>For create flows, the incoming input flag wins when no aggregate exists yet.</li>
 *   <li>For cross-aggregate flows, the primary aggregate of the invoked use case wins.</li>
 * </ol>
 *
 * <p>{@link #NONE} is the Null Object and is always the default branch.</p>
 */
public enum FlagEnum {
    NONE,
    BULLET_IGNORE_SUBSCRIPTION_RESERVATION,
    SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION
}
