---
name: installments-review-findings
description: 2026-05-12 installment findings — RESOLVED by 2026-07-06 (transactional decorators, WalletDeductionsQuery port, Expense.installmentId back-link, tests green)
metadata:
  type: project
---

2026-05-12 findings verified resolved on 2026-07-06 full review:
- Failing E2E: unit suite fully green (1135 tests; ITs skipped in that run).
- DIP cascade into wallet use cases: replaced by `WalletDeductionsQuery` port + `RepositoryBackedWalletDeductionsQuery` with per-owner-month batch caches (also fixes the N+1 in `FindAllWalletsUseCase`).
- 3-write installment save without atomic guarantee: `TransactionalSaveExpenseBoundary` (+23 sibling decorators in `infra/configs/transactional/`) + `mongoTransactionsReplicaSetGuard` fail-fast.
- Missing Expense↔Installment back-link: `ExpenseDocument.installmentId` + `Installment.sourceExpenseId` both present, with `expense_owner_installment_idx`.

**How to apply:** don't re-report these; current open issues live in [[architectural-findings]].
