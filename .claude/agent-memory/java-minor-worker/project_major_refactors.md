---
name: Major Refactors Applied (subscriptions-code-review.md)
description: 5 major issues from subscriptions-code-review.md applied May 2026 — template method strategies, flag executor parity, null object, registry builder, config split
type: project
---

All 5 pending Major issues from `subscriptions-code-review.md` were applied (Major 2 was pre-fixed):

- **Major 1** — `SaveBulletUseCaseSupport` deleted; `AbstractSaveBulletStrategy` (template method) introduced; `DefaultSaveBulletStrategy` and `SaveBulletIgnoreSubscriptionReservationStrategy` now extend it.
- **Major 6** — `PatchBulletUseCase` now routes through `FlagAwareExecutor<PatchBulletInput, BulletOutput>`; `AbstractPatchBulletStrategy`, `DefaultPatchBulletStrategy`, `PatchBulletIgnoreSubscriptionReservationStrategy` mirror the save-bullet pattern.
- **Major 4** — `NoSubscriptionRepository` (null object) added in domain; all optional `SubscriptionRepository` constructors removed from wallet use cases; `WalletSubscriptionSelector` null-check removed.
- **Major 5** — `FlagStrategyRegistry.Builder<I,O>` added; configuration now uses `.builder().withDefault(...).register(...).build()`.
- **Major 3** — `BusinessLayerBeanConfiguration` (321 lines) split into `BulletBeanConfiguration`, `WalletBeanConfiguration`, `SubscriptionBeanConfiguration`, `ExpenseBeanConfiguration`, `PaymentBeanConfiguration`, `SharedBeanConfiguration` — all under `infra/configs/`.

**Why:** Code review requirement for clean architecture, DIP compliance, flag-precedence consistency.
**How to apply:** All six new configs are discovered by Spring component scan. Known pre-existing flaky test: `SubscriptionChargeRepositoryImplTest.save_whenSame...throwsDuplicateKeyException` — out of scope.
