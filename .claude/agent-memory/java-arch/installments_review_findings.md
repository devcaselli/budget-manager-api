---
name: installments-review-findings
description: 2026-05-12 review of installments feature (commits after 4534a0a8) — failing E2E, DIP cascade into wallet usecases, transactional gaps, missing Expense↔Installment back-link.
metadata:
  type: project
---

Installments feature review (commits c68d46a back to 6ecc458) persisted to `code-review-installments.md` at project root.

Standing issues to remember (likely to come back in future conversations):

1. **`InstallmentBusinessRulesEndToEndTest` fails on `main` — bug is in the test**, not production. Wallet remaining rule (baseline 4534a0a8): `budget − subscriptions − installments(where sourceEffectiveMonth != walletEffectiveMonth)`. Expenses never debit wallet. Source-month wallet correctly shows full budget. E2E asserts 4000 but should assert 10000 on May wallet. Unit test `installmentTotal_sourceWallet_returnsZero` is correct. Fix: change E2E line 83 to `value(10000.00)`.

2. **`SaveExpenseUseCase` installment branch does 3 writes** (hidden source `Expense` → `Installment` → per-installment `Expense`) under `@Transactional` via `TransactionalSaveExpenseBoundary`. Mongo transactions need replica set — stand-alone mongo silently downgrades. Testcontainers `MongoDBContainer("mongo:7.0")` is stand-alone by default, so the transactional guarantee is not actually being tested.

3. **No back-link `Expense → Installment`.** `DeleteInstallmentUseCase` soft-deletes only the `Installment`; the per-installment `Expense` rows remain live and still surface through `GET /credit-cards/{id}/expenses`. No way to reconcile partial-save state either. Adding `Expense.installmentId` would solve both.

4. **DIP cascade into wallet usecases:** `FindAllWalletsUseCase`, `FindWalletByIdUseCase`, `PatchWalletUseCase`, `SaveWalletUseCase` all take `InstallmentRepository` directly. Should be hidden behind a `WalletDeductionsQuery` port that fans out to subscription + installment calculators. This is the same DIP-cascade pattern flagged in [[payment_module_issues]].

5. **N+1 in `FindAllWalletsUseCase.toOutput`** — one `findActiveAffecting` per wallet. TODO is in the code itself. Subscription cache pattern shows how to fix.

6. **Migration `2026-05-11_wipe-expense-add-creditCardId.js`** uses `count > 50` heuristic guard — no env check, no backup, no rollback. Operator-trap waiting to happen.

7. **`Expense.equals` includes `flag`** — breaks `Set.contains` after patching flag. Identity should be `id` only (matches `Installment.equals` and `CreditCard.equals`).

8. **`Installment.validateInvariants` rejects `purchaseDate.isAfter(LocalDate.now())`** — couples invariant to system clock, same anti-pattern as `Expense.create`. Should accept a `Clock` like `DeleteInstallmentUseCase` does.

**How to apply**: Before citing these on a new conversation, run the E2E test or read the touched files — items #1 and #2 are deeply contested between code, test, and spec, so confirming current state matters before quoting numbers.
