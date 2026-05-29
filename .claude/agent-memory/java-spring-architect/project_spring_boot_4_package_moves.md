---
name: Spring Boot 4 + Jackson 3 package moves
description: Known package relocations that break copy-pasted Spring Boot 3 examples in this project
type: project
---

Projeto usa **Spring Boot 4.0.5** (não 3.x) e **Jackson 3** (não 2.x). Isso quebra praticamente qualquer exemplo da internet feito para Boot 3. Relocations conhecidos encontrados durante a Task 08:

- `@WebMvcTest`: `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` → **`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`**
- `AutoConfigureMockMvc`: mesmo prefixo novo (`org.springframework.boot.webmvc.test.autoconfigure`)
- `ObjectMapper` (Jackson): `com.fasterxml.jackson.databind.ObjectMapper` → **`tools.jackson.databind.ObjectMapper`**
- Todo o ecossistema Jackson moveu de `com.fasterxml.jackson.*` para `tools.jackson.*` (ver `~/.m2/repository/tools/jackson/`).
- Starter renomeado: `spring-boot-starter-test` convencional foi substituído por `spring-boot-starter-webmvc-test` no módulo `infra` (já presente no pom).

**Why:** copiar qualquer snippet de Stack Overflow / docs Boot 3 vai quebrar o compile com "package does not exist" ou "cannot find symbol WebMvcTest". A mensagem de erro não é óbvia porque a IDE autocomplete sugere o caminho antigo.

**How to apply:** antes de escrever qualquer teste Spring Boot novo neste projeto, assumir que os packages `test.autoconfigure.web.servlet` e `com.fasterxml.jackson.*` **não existem** aqui. Se a IDE importar automaticamente do path antigo, corrigir manualmente para o novo. Ao investigar "cannot find symbol" em testes, rodar `unzip -l` no starter pom correspondente em `~/.m2/` para ver o package atual.
