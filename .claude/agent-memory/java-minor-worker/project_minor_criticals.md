---
name: Minor Worker Critical Fixes
description: 3 criticals + Codex residuals from subscriptions-code-review.md second pass (applied 2026-05-07)
type: project
---

Critical 1 (SaveBulletUseCase): Reduced to single constructor (executor + findWallet). Legacy 3-arg and 4-arg constructors deleted. SaveBulletUseCaseTest re-wired with real DefaultSaveBulletStrategy + mocked SubscriptionRepository + no-op FlagManager. TransactionalSaveBulletBoundaryTest re-wired with SaveBulletIgnoreSubscriptionReservationStrategy to avoid needing a SubscriptionRepository in that integration context.

Critical 2 (EndMonthBeforeStartMonthException): New typed exception in domain/subscription/exception. Both Subscription constructor and endAt() now throw it. DeleteSubscriptionUseCase catches by type, not string. Static constant removed.

Critical 3 (test YAML): Created infra/src/test/resources/application.yaml with auto-index-creation: true. Unblocked SubscriptionChargeRepositoryImplTest duplicate-key test.

Codex A: FlagStrategyRegistry.Builder.register now rejects FlagEnum.NONE with IllegalArgumentException.
Codex B: Added 4 builder tests to FlagStrategyRegistryTest.
Codex C: Double bullet read in PatchBulletUseCase documented with comment explaining transactional boundary rationale.
Codex D: FlagAwareExecutor log level kept at INFO (no change needed).

**Why:** Code review mandated typed exceptions, API cleanup, test reliability.
**How to apply:** These are now applied. Do not re-introduce legacy SaveBulletUseCase constructors or the string-matching catch in DeleteSubscriptionUseCase.
