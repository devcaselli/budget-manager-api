---
name: architectural-findings
description: Review history — 2026-04-14 findings all resolved; 2026-07-06 full review lists current standing issues (N+1 enricher, handler OCP, refresh-token expiry/plaintext, DIP leftovers)
metadata:
  type: project
---

# Review history

## 2026-04-14 findings — ALL RESOLVED (verified 2026-07-06)
- `Bullet.paymentId`: gone (field removed).
- Mongo URI: was false positive; dev yaml now uses `?replicaSet=rs0` + fail-fast `mongoTransactionsReplicaSetGuard`.
- Payment currency: fixed — `Payment` uses `Money` VO (HALF_EVEN, scale 2) everywhere.
- PayExpense atomicity: `TransactionalPayExpenseBoundary` + `PaymentPolicy.validate` (wallet/currency cross-checks).
- DIP cascade in wallet: fixed via `WalletDeductionsQuery` port + `RepositoryBackedWalletDeductionsQuery` batch caches.
- SaaS gaps: ownerId multi-tenancy, `@Version` optimistic locking, `@Indexed`/`@CompoundIndex`, Spring Security (dual filter chains, JWT RSA + jti revocation + audience validator), CORS restrito, rate-limit filter — all present now.

## 2026-07-06 full review — standing issues (1135 unit tests green, ITs not run)
1. **N+1** `ExpenseOutputEnricher.installmentNumbers` (application/expense/usecase) — per-id `findById` loop; `InstallmentRepository.findAllByIds` exists but unused.
2. **OCP** `GlobalExceptionHandler` 621 lines, ~15 copy-paste NotFound + Conflict handlers — needs common base exception + single handler.
3. **Refresh token**: `RefreshUseCase`/`RefreshTokenRepositoryImpl` never check `expiresAt` at read (relies on lazy Mongo TTL, ≥60s lag) + token stored plaintext as `_id` (should store SHA-256 hash).
4. **DIP leftovers**: `MaterializePluggyTransactionsUseCase` ctor does `new FetchPluggyTransactionsForItemUseCase(...)`; `SaveExpenseUseCase` convenience ctor does `new InstallmentExpenseSaver(...)`.
5. Nullable `shareRepository` param overloads in `SubscriptionWalletBalanceCalculator`/`InstallmentWalletBalanceCalculator` (null-as-optional-dependency smell).

**How to apply:** on next review, verify items 1–5 before re-flagging; wallet deduction batching is already O(distinct owner-month keys) — don't flag it.
