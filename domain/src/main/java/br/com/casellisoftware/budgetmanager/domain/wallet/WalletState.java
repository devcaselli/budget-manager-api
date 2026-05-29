package br.com.casellisoftware.budgetmanager.domain.wallet;

/**
 * Lifecycle state of a {@link Wallet}.
 *
 * <ul>
 *   <li>{@code PRODUCTION}: the canonical wallet for an {@code effectiveMonth}.
 *       Only one open PRODUCTION wallet is allowed per {@code effectiveMonth}.</li>
 *   <li>{@code REVIEW}: terminal state reached from a closed PRODUCTION wallet —
 *       used for read-only audit. No further transitions allowed.</li>
 *   <li>{@code PREVIEW}: simulation/sandbox wallet that receives charges but is
 *       not the canonical month wallet. No uniqueness restriction.</li>
 * </ul>
 */
public enum WalletState {
    PRODUCTION,
    REVIEW,
    PREVIEW
}
