---
name: Architectural findings (full-project review)
description: Full-project architectural review dated 2026-04-14 — bugs, inconsistencies, DIP violations, and SaaS-readiness gaps. Superseded by code changes; verify before citing.
type: project
---

Full architectural review completed on 2026-04-14 and persisted to `RAIO-X_ARQUITETURAL_20260414.md` at the project root. Overall verdict: codebase is solid **Sênior** level; to reach **Sênior 2** it needs concurrency/tx/multi-tenant/observability work.

**The standing architectural issues below are things that live across the project — not pinpoint fixes — so they are likely to come up again in conversations:**

1. **`Bullet.paymentId` is singular** — overwrites on every `Bullet.pay(Payment)`. Either make it a list (symmetric to `Expense.paymentIds`) or drop the field; the reverse relation already lives on `Payment.bulletId`.

2. **`spring.mongodb.uri` in `application-dev.yaml` is wrong** — Spring Boot reads `spring.data.mongodb.uri`; current key is silently ignored and the dev app connects to the default fallback URI. Trivial fix, very easy to miss.

3. **`PaymentPersistenceMapper` loses currency AND is the only MapStruct mapper without `unmappedTargetPolicy=ERROR`.** `Payment` uses `BigDecimal amount` directly (no `Money`), unlike Expense/Bullet/Wallet. If we model Payment with `Money`, we buy invariant "payment.currency == expense.currency == bullet.currency".

4. **`PayExpenseUseCase` orchestrates 3 aggregates without `@Transactional` and without cross-aggregate invariants.** No check that `bullet.walletId == expense.walletId`, no currency match, no idempotency. Payment can persist while the Expense/Bullet debit fails, leaving orphan state.

5. **DIP cascade violations:** `SaveExpenseUseCase`, `SaveBulletUseCase`, `DeleteExpenseByIdUseCase` inject concrete use-case classes (e.g., `FindWalletByIdUseCase`) instead of boundary interfaces. `FindAllPaymentByExpenseIdBoundary` is the only one done right so far. Boundaries exist for most use cases (`FindExpenseByIdBoundary`, `PatchBulletBoundary`, etc.) — they just aren't being consumed.

6. **`FindWalletByIdUseCase` does not implement `FindWalletByIdBoundary`.** The boundary declares `Optional<WalletOutput> findById(String)`; the use case has `WalletOutput execute(String)` (throws). The boundary is dead code. Pick one contract.

7. **`PatchHelper` reflection bomb** (already in prior memory, still open). Adding a new field to an entity while forgetting to align constructor order → silent miswrite. Best resolved by per-entity explicit `patch(input)` methods.

8. **`SavePaymentUseCase` is dead code** — still wired as `@Bean` but no controller uses it. Delete it.

9. **Testing gaps:** no tests for `PaymentController`, `PayExpenseUseCase`, `DeleteExpenseByIdUseCase`, `PatchHelper`, or any of the patch use cases. `DeleteExpenseByIdUseCase` is the most complex orchestrator in the codebase and is the least tested.

10. **Missing for SaaS:** no multi-tenant/`ownerId`, no `@Version`/optimistic locking, no `@Indexed` on queried fields, no Spring Security, no idempotency keys, no observability (Micrometer/OTel), no OpenAPI spec, no migrations (Mongock), no CORS config.

11. **Inconsistent `WalletDocument`:** persists `Money` in-place while `ExpenseDocument`/`BulletDocument` flatten to `amount + currency` — different JSON shape for sibling collections.

12. **Inconsistent NotFoundException constructors:** Expense/Payment take `(String id)` with auto-wrap; Bullet takes `(String message)`; Wallet has both. Standardize on `(String id)` with wrap inside the constructor.

**How to apply:** Before recommending any fix, verify the file still exists and the shape is still the same — this memory reflects state at 2026-04-14 and some items will be gone by the time they're referenced. For architecture-level suggestions, these gaps are likely to still be around for a while (multi-tenant and transactions especially). Prioritize by the 3-wave plan in the root document: correção → robustez → plataforma SaaS.
