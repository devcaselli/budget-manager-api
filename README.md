# Budget Manager API

Spring Boot service for managing personal budgets across wallets, expenses, installments, subscriptions, credit cards, and shared expenses.

## Architecture

Three-module Maven reactor with strict inward-pointing dependencies:

```
infra → application → domain
```

- **`domain/`** — pure Java aggregates, ports (repository interfaces), value objects, and domain exceptions. Zero framework imports.
- **`application/`** — use cases as plain classes wired via `@Bean` in `infra/` (see [`application/README.md`](application/README.md)). Zero Spring imports.
- **`infra/`** — Spring Boot adapters: REST controllers, MongoDB persistence, MapStruct mappers, security, and transactional decorators.

## Modules

| Subdomain | Highlights |
|-----------|------------|
| `wallet` | Monthly budget envelopes with bullets and subscription reservations |
| `expense` | One-shot expenses tied to wallet + credit card; can be hidden (off-balance) when fully shared |
| `installment` | Multi-month expense splits with materialization rules |
| `subscription` | Recurring charges with effective months and per-version pricing; requires a `creditCardId` |
| `subscriptioncharge` | Materialized monthly charges (preview + persisted) |
| `creditcard` | Credit card registry; cascade-blocking on referencing expenses, installments, or active subscriptions |
| `payer` | Lifecycle-typed payer registry (`STANDING` / `TRANSIENT`); wallet-scoped for transient |
| **`sharing`** | Ratio-based splits of expense / subscription / installment among payers + owner |
| `payment` | Immutable ledger; `kind` distinguishes `NORMAL` vs `SHARED`; reversals link to the original payment |

## Sharing module

`Share` aggregates divide or fully assign a source (expense, subscription, installment) among one or more payers with an optional owner share. Stored as ratios so subscription/installment materializations apply the split automatically each month.

- One **ACTIVE** share per source is enforced by both the use case and a Mongo partial unique index.
- Full assignment (`ownerShare == 0`) marks the source expense `hidden` (off-balance for owner balances). Revert restores visibility.
- Reverts emit reversal payments (`reversal=true`, `reversedPaymentId=<original>`) instead of deleting — the ledger is append-only.
- Subscription charge previews are enriched with `shared` and `effectiveOwnerAmount` so the UI can render the owner's share without a second round trip.

See [`FEATURE_POWERFUL_PAYMENTS.md`](FEATURE_POWERFUL_PAYMENTS.md) for the full design, scenarios, and Big-O notes.

## MongoDB requirements

Some sharing use cases rely on multi-document transactions through Spring's `@Transactional` decorators in `infra/configs/transactional/`. **MongoDB must run as a replica set** (single-node replica set is fine) — standalone MongoDB does not support transactions. Local dev via Docker:

```bash
docker run -d --name budget-mongo \
  -p 27017:27017 \
  mongo:7.0 \
  --replSet rs0
docker exec budget-mongo mongosh --eval "rs.initiate()"
```

Test suites use [`MongoDBContainer`](https://www.testcontainers.org/modules/databases/mongodb/) which starts a replica-set-enabled container automatically.

## Build & test

```bash
./mvnw clean verify
```

Runs unit tests in `domain/` and `application/`, plus `@WebMvcTest`, `@DataMongoTest`, and full Testcontainers-backed end-to-end tests in `infra/`.

## Migrations

Mongo migrations live in `infra/src/main/resources/db/migration/` and are idempotent. Apply them manually via `mongosh` before booting a new environment.

## Related docs

- [`FEATURE_POWERFUL_PAYMENTS.md`](FEATURE_POWERFUL_PAYMENTS.md) — Powerful Payments feature plan (Payer lifecycle, Sharing subdomain, Payment.kind, Subscription+CreditCard, Expense off-balance)
- [`application/README.md`](application/README.md) — bean-wiring rule for the framework-free application layer
- [`docs/security-plan.md`](docs/security-plan.md) — security review notes
