---
name: Project conventions (budget-manager-api)
description: Non-obvious architectural and wiring conventions used across the budget-manager-api multi-module project
type: project
---

Conventions that are non-obvious from a quick read and worth remembering before suggesting changes:

**Module layout:** `domain/` (pure Java, no Spring), `application/` (use cases + boundaries, no Spring), `infra/` (controllers, persistence, Spring-managed beans). Dependency direction is strictly `infra → application → domain`.

**Use cases are plain classes, NOT @Service.** They are wired explicitly as `@Bean` methods inside `BusinessLayerBeanConfiguration` in the infra module. Do not add `@Service`/`@Component` to use cases — that breaks the convention.

**Constructor injection only.** No field injection anywhere.

**DTOs are records.** `*Input`, `*Output` (boundary layer), `*RequestDto`, `*ResponseDto` (REST layer) are all Java records. Don't propose classes for new DTOs.

**Assemblers are hand-written, not MapStruct** — for domain ↔ boundary DTOs. MapStruct is reserved for REST DTO ↔ boundary DTO (strict policy: `unmappedTargetPolicy = ERROR`) and domain ↔ persistence document.

**Domain entities are rich and immutable.** State transitions return new instances (e.g., `Expense.debit(Money)`, `Expense.pay(Payment)`). Factory methods (`Expense.create(...)`) enforce invariants. No Lombok on domain types; hand-written `equals`/`hashCode`.

**Persistence is MongoDB via Spring Data.** `*RepositoryImpl` in infra wraps a `*MongoRepository extends CrudRepository`. Domain port returns `Optional`; "not found" is never an exception at the repository layer.

**`@Transactional` lives only in infra decorator beans.** The pattern (introduced after the 2026-04-14 raio-x) is: keep the use case in `application/` Spring-free, then wrap it in a `Transactional<UseCaseName>Boundary` decorator under `infra/configs/transactional/` that implements the same boundary and annotates `execute(...)` with `@Transactional`. The bean factory in `BusinessLayerBeanConfiguration` exposes the decorator (not the raw use case) under the boundary type. Existing examples: `TransactionalPayExpenseBoundary`, `TransactionalDeleteExpenseBoundary`. Use the same pattern for any new multi-aggregate orchestrator.

**Why:** These conventions were established through multiple code-review rounds (commits "Fixing code review points 01–10"). Deviating from them signals tech debt in this repo, not a neutral choice.

**How to apply:** When adding a new module (e.g., another domain like Category, Tag), mirror the Wallet/Expense/Bullet structure exactly: domain port + entity, application use cases + boundaries + record DTOs + hand-written assembler, infra repository impl + @Bean wiring in `BusinessLayerBeanConfiguration`, and a REST controller that depends only on boundary interfaces.
