---
name: @WebMvcTest with nested dummy controllers needs explicit @Import
description: Gotcha when writing WebMvcTest slices with a dummy controller declared as a nested static class
type: feedback
---

Ao escrever `@WebMvcTest` com um **dummy controller aninhado** (static nested class no próprio test), `@WebMvcTest(controllers = NestedController.class)` **não basta** para registrar o controller como bean. Requests batem em `NoResourceFoundException` e caem no catch-all 500, mascarando o problema real.

É preciso adicionar o nested controller também ao `@Import`:

```java
@WebMvcTest(controllers = MyTest.DummyController.class)
@Import({GlobalExceptionHandler.class, MyTest.DummyController.class})
class MyTest { ... }
```

**Why:** `@WebMvcTest(controllers = X)` só **restringe** o filtro de scanning a X — não faz o scanning incluir classes nested. Gastei uma rodada de debug na Task 08 com 4 testes falhando em 500 até descobrir que todos os requests estavam com `NoResourceFoundException` (rota `/dummy/*` não registrada).

**How to apply:** sempre que criar um `@WebMvcTest` com dummy controller nested neste projeto (Task 09 vai replicar esse padrão para testar o `SaveExpenseEntrypointImpl`), incluir o controller no `@Import`. Se os testes voltarem 500 inesperados, a primeira hipótese é "controller não registrado" — confirma adicionando temporariamente `problem.setProperty("debugExceptionClass", ex.getClass().getName())` no handler genérico; se aparecer `NoResourceFoundException`, é isso.
