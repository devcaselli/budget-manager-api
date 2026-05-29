---
name: project-architecture
description: Multi-module Maven layout, Spring Boot 4.0.5, Java 21, MongoDB, key package conventions
metadata:
  type: project
---

Multi-module Maven project at `/home/victor/Projects/personal/budget-manager-api`.

Modules: `domain/`, `application/`, `infra/`

Base packages:
- domain: `br.com.casellisoftware.budgetmanager.domain`
- application: `br.com.casellisoftware.budgetmanager.application`
- infra: `br.com.casellisoftware.budgetmanager` (root, no `.infra` segment)

Maven wrapper at project root: `./mvnw`. Run tests with `./mvnw test`.

Spring Boot 4.0.5, Java 21, MongoDB (Spring Data MongoDB), MapStruct, Lombok (infra only).

**Why:** application/ must have ZERO Spring imports — enforced by convention, not a build plugin.

**How to apply:** Never add Spring annotations or imports to `application/` module classes.
