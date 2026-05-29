---
name: coding-conventions
description: Project-specific Java coding conventions enforced by code reviews
metadata:
  type: project
---

- **Use cases**: Plain classes (NOT @Service). Wired via `@Bean` in `*BeanConfiguration` classes in `infra/configs/`.
- **DI**: Constructor injection only. No field injection (@Autowired on fields).
- **DTOs**: Java records. Named `*Input`, `*Output` in application; `*RequestDto`, `*ResponseDto` in infra/rest.
- **Domain entities**: No Lombok. Hand-written equals/hashCode. Immutable (state transitions return new instances). Factory method `create(...)`.
- **Assemblers**: Hand-written for domain↔boundary. MapStruct only for REST DTO ↔ boundary DTO.
- **@Transactional**: Only on infra decorator beans (not on use cases directly).
- **MapStruct config**: `ProjectMapper` interface sets `componentModel="spring"`, `injectionStrategy=CONSTRUCTOR`, `unmappedTargetPolicy=ERROR`, `unmappedSourcePolicy=ERROR`. REST mappers that have field name mismatches should NOT inherit ProjectMapper to avoid unmappedSourcePolicy conflicts — use inline `@Mapper(componentModel="spring", unmappedTargetPolicy=ERROR)`.
- **Persistence mappers**: Use `default` methods when domain entity has no setters (immutable). MapStruct still generates a Spring @Component.
- **Bean configs**: Live in `infra/src/main/java/.../configs/`. Pattern: `@Configuration` class with `@Bean` methods wiring use cases to boundary interfaces.

**Why:** Clean Architecture separation. application/ stays portable and testable without Spring.
