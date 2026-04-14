---
name: Payment module open issues
description: Remaining architectural debt in the payment module after the 2026-04-13 refactor — PatchHelper reflection is the only substantive item left
type: project
---

Review of payment module (originally introduced in commits 8340b21 and 8be59b0) yielded several architectural findings. Resolution status as of 2026-04-13:

**Resolved in 2026-04-13 session:**

1. ✅ `PaymentService` (infra) orchestrating across Expense/Bullet/Payment aggregates → **replaced** by `PayExpenseUseCase` in `application/payment/usecase/`, which talks to domain repositories directly and invokes `Expense.pay()` / `Bullet.pay()` on the real entities (no Output→Domain conversion). `PaymentService.java` and the `service/` package were deleted.
2. ✅ REST DTOs bypassing a boundary Input → `PaymentRestMapper` now exposes `toPayExpenseInput(PayRequestDto, String walletId)` with strict MapStruct policies (ERROR on unmapped source/target), matching the Expense/Bullet mappers.
3. ✅ `FindAllPaymentByExpenseIdUseCase` without boundary → added `FindAllPaymentByExpenseIdBoundary`; `DeleteExpenseByIdUseCase` now depends on the abstraction.
4. ✅ Class-name typo `SaveSavePaymentUseCase` → renamed to `SavePaymentUseCase` (git mv so rename history is preserved).
5. ✅ Dead import `PaymentRepository` in `PaymentOutputAssembler` → removed.

**Still open:**

- **`PatchHelper` uses reflection** (`application/shared/PatchHelper.java`) to patch domain entities from record inputs. Relies on the entity's constructor parameter order matching its field declaration order AND on record accessor names matching entity field names exactly. Silent failures possible if either side drifts. **Why deferred:** touches Expense + Bullet patch flows simultaneously; deserves its own pass with tests added first.
- **`SavePaymentUseCase` is dead code** (no controller wires it; `/pay` uses `PayExpenseUseCase`). It still uses `Instant.now()` instead of an input-provided `paymentDate`. **Why deferred:** user may have plans for it; removing it wasn't in the refactor scope.

**How to apply:** If the user brings up `PatchHelper`, treat the refactor as non-trivial — propose explicit per-entity patch methods rather than tightening the reflection contract. If they ask about `SavePaymentUseCase`, confirm whether to delete it or wire a new endpoint.
