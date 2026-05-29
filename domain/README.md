# domain module

Pure domain layer — no framework dependencies. Entities are rich (validated
constructors, encapsulated behavior, immutable value objects).

## Deliberate removals

- **`Wallet`** — removed in Task 12 (2026-04-10). The class was orphaned: no
  repository port, no use case, no tests, no references. `Expense.walletId`
  remains as an opaque `String` reference. When a Wallet feature is needed,
  reintroduce it as a full aggregate (rich entity + port + use case + tests).
