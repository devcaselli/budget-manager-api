---
name: No cron/reconciler for subscription charges
description: User rejected scheduled reconciler approach for subscription→wallet materialization; demands purely synchronous backfill mirroring SaveBulletUseCase
type: feedback
---

Subscription charges must materialize synchronously inside Save/Patch subscription use-cases, following the exact pattern of `SaveBulletUseCase`: `BulletAllocationPolicy.validateAllocation` → `wallet.debit` → `walletRepository.save` → `chargeRepository.save`, all inside the existing `Transactional*Boundary` decorator.

**Why:** User explicitly rejected the cron + virtual-thread reconciler design ("ficou péssima em todos os sentidos") on 2026-05-06 after it was implemented. Preference: lean on the existing transactional bullet-allocation pattern instead of adding scheduled background reconciliation.

**How to apply:** When extending sub→wallet linking (or any analogous "materialize on event" flow) do not propose `@Scheduled`, virtual-thread executors, or defensive reconcilers. Wire it inline into the write-path use-case, reuse `BulletAllocationPolicy`, and rely on optimistic locking + unique indexes for race safety.
