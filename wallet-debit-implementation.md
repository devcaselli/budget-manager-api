# Wallet Debit on Bullet Lifecycle — Implementation Plan

> **Escopo:** quando um `Bullet` é criado, alterado ou removido, a `Wallet`
> dona deve ter o `remaining` reconciliado de forma atômica. Hoje
> `SaveBulletUseCase` apenas valida a existência da wallet; nenhuma
> escrita acontece em `WalletDocument`. Esse plano implementa o caminho
> A discutido na conversa (Wallet com `remaining` materializado, espelhando
> `PayExpenseUseCase`).

---

## 0. Golden Rules (regras inegociáveis para essa entrega)

Cada decisão e cada PR deve passar por todas estas regras. Se uma proposta
quebrar alguma, o caminho está errado.

1. **Use case = orquestração, domínio = invariante.** A regra "não pode
   estourar a wallet" é uma invariante de domínio e mora numa
   `BulletAllocationPolicy`, espelhando `PaymentPolicy`. A use case só
   chama `policy.validate(...)` e persiste.
2. **Atomicidade via decorator infra, nunca via `@Transactional` em
   `application`.** Multi-aggregate writes ⇒ um `Transactional<Boundary>`
   no pacote `infra/configs/transactional/`. O `application/` continua
   100% Spring-free.
3. **Imutabilidade preservada.** `Wallet.debit(...)` e o futuro
   `Wallet.credit(...)` retornam nova instância; nada de setters.
4. **DIP, sem cascade.** Use cases novas/alteradas só dependem de
   *boundaries* (`FindWalletByIdBoundary`, `SaveWalletBoundary`,
   `BulletRepository`). **Nenhum `new` de outra use case e nenhum
   `*UseCase` injetado por classe concreta.** Esse é o item 5 do
   raio-x — não replicar a dívida.
5. **Saldo da wallet é leitura O(1).** `Wallet.remaining` continua
   sendo estado materializado. Em nenhum ponto do fluxo deve aparecer
   "soma todos os bullets para descobrir o saldo".
6. **Ordem de escrita estabelecida.** Dentro da transação:
   `(1) save Wallet` → `(2) save Bullet`. Justificativa em §6.
7. **Não há refund de bullet com `remaining < budget`.** Se o bullet já
   foi parcialmente pago, o que volta para a wallet é o `remaining`,
   nunca o `budget` original. Caso contrário, a soma do que voltou +
   pagamentos efetuados excederia o orçamento original — dinheiro
   "criado".
8. **Currency match obrigatório.** A policy rejeita `bullet.budget`
   com moeda diferente de `wallet.budget`. O `Money.debitBy(...)` já
   lança em mismatch, mas a policy emite uma exceção semântica antes,
   com mensagem útil.
9. **Patch só altera `budget` se a redução couber em `remaining`.** A
   parte já consumida do bullet (paga) é intocável.
10. **Tudo o que entra em produção tem teste — incluindo o caminho
    transacional.** Cobertura mínima descrita em §10.

---

## 1. Modelo conceitual

```
Wallet (envelope-mãe)
 ├── budget       : Money  // orçamento total
 └── remaining    : Money  // ainda não comprometido em bullets

Bullet (sub-envelope alocado a partir da wallet)
 ├── budget       : Money  // valor alocado da wallet (compromete remaining da wallet)
 ├── remaining    : Money  // ainda não consumido por pagamentos
 └── walletId     : String // dono

Expense, Payment: consomem `bullet.remaining` (nunca tocam wallet diretamente)
```

Invariantes globais que o sistema deve manter sempre:

- `wallet.remaining >= 0`
- `wallet.remaining = wallet.budget - Σ bullet.budget` para bullets ativos
  (mesma wallet, não deletados).
- `bullet.remaining <= bullet.budget`
- `bullet.remaining >= 0`
- `bullet.budget.currency == wallet.budget.currency`

> Esse plano materializa as duas primeiras invariantes via writes
> coordenadas. As três últimas já existem hoje no domínio.

---

## 2. Visão geral das mudanças (mapa)

| Camada | Pacote | Arquivos novos | Arquivos alterados |
|---|---|---|---|
| domain | `domain.wallet.policy` | `BulletAllocationPolicy.java` | — |
| domain | `domain.wallet.exception` | `WalletAllocationExceededException.java`, `WalletCurrencyMismatchException.java` | — |
| domain | `domain.wallet` | — | `Wallet.java` (adicionar `credit`) |
| application | `application.wallet.boundary` | `FindWalletDomainByIdBoundary.java` | — |
| application | `application.wallet.usecase` | `FindWalletDomainByIdUseCase.java` | — |
| application | `application.bullet.usecase` | — | `SaveBulletUseCase.java`, `PatchBulletUseCase.java` |
| application | `application.bullet.usecase` | `DeleteBulletByIdUseCase.java` | — |
| application | `application.bullet.boundary` | `DeleteBulletByIdBoundary.java` | — |
| infra | `configs.transactional` | `TransactionalSaveBulletBoundary.java`, `TransactionalPatchBulletBoundary.java`, `TransactionalDeleteBulletByIdBoundary.java` | — |
| infra | `configs` | — | `BusinessLayerBeanConfiguration.java` |
| infra | `rest.bullet` | — | `BulletController.java` (`PATCH`, `DELETE`) |
| domain | `domain.bullet` | — | `Bullet.java` (helper `consumed()` opcional) |
| testes | múltiplos | espelho de cada um | atualizar `SaveBulletUseCaseTest`, `PatchBulletUseCaseTest`, `BulletControllerTest` |

> Não confundir `FindWalletDomainByIdBoundary` (retorna o agregado
> `Wallet` pronto para mutar) com `FindWalletByIdBoundary` (já existente,
> retorna `WalletOutput` para a borda REST). Justificativa detalhada em §3.2.

---

## 3. Decisões arquiteturais

### 3.1 Por que uma `BulletAllocationPolicy` no domínio (e não dentro da use case)

`PaymentPolicy.validate(...)` já estabelece o padrão: invariantes
multi-agregadas viram policies estáticas em `domain/<aggregate>/policy/`.
Replicar esse padrão para a alocação:

- Mantém SRP nas use cases (orquestração apenas).
- Permite testar a regra em isolamento, sem mocks de repositório.
- Mensagens de erro consistentes com as do pagamento.

### 3.2 Por que um novo boundary `FindWalletDomainByIdBoundary`

`FindWalletByIdBoundary` retorna `WalletOutput` (DTO achatado com
`BigDecimal`). Para escrever no agregado, a use case precisa do `Wallet`
de domínio, não do DTO.

Opções avaliadas:

| Caminho | Custo | Risco |
|---|---|---|
| (a) Injetar `WalletRepository` direto na use case | Baixo | **Reproduz o item 5 do raio-x (DIP cascade).** |
| (b) Reaproveitar `FindWalletByIdBoundary`, fazer round-trip `WalletOutput → Wallet` | Médio | Cria assembler reverso só para essa use case; perde imutabilidade implícita. |
| (c) Novo boundary `FindWalletDomainByIdBoundary` que devolve `Wallet` agregado | Médio | Limpo. Único downside: mais um arquivo. |

**Vencedor: (c).** O argumento decisivo é a regra dourada #4: a use case
de domínio precisa do agregado de domínio, não de uma projeção. Expor
um boundary específico é a forma DIP-correta.

> Alternativa pragmática válida: enriquecer `FindWalletByIdBoundary`
> com um segundo método `findDomainById(String): Wallet`. Não recomendo
> porque o primeiro método retorna DTO e o segundo entidade — viola ISP
> ("clientes obrigados a depender de método que não usam").

### 3.3 Por que decoradores transacionais por operação

O projeto já tem dois (`TransactionalPayExpenseBoundary`,
`TransactionalDeleteExpenseBoundary`). Cada operação de bullet vira um
decorator próprio. Vantagens:

- `application/` continua sem dependência de Spring.
- A transação fica explícita e auditável (uma classe = uma fronteira).
- Permite no futuro plugar Micrometer / OTel exatamente nesse ponto sem
  poluir o domínio.

### 3.4 Por que NÃO seguir o caminho B (wallet derivada)

Já analisado na conversa anterior. Síntese aqui só para servir de
ata para futuros leitores:

- Wallet hoje **tem `remaining` materializado** — derivar é refactor de
  alto custo (mapper, document, output, REST, testes).
- Operações ficam O(n) em bullets (n cresce em SaaS).
- Não resolve a corrida concorrente, que é o real problema de robustez
  e cabe a `@Version` + retry — solução universal para A e B.

---

## 4. Domain layer — código

### 4.1 `domain/wallet/exception/WalletAllocationExceededException.java`

**Caminho exato:**
`domain/src/main/java/br/com/casellisoftware/budgetmanager/domain/wallet/exception/WalletAllocationExceededException.java`

```java
package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

/**
 * Lançada quando o orçamento solicitado para um bullet excede
 * o {@code remaining} da wallet à qual pertence.
 */
public class WalletAllocationExceededException extends RuntimeException {

    public WalletAllocationExceededException(String message) {
        super(message);
    }
}
```

### 4.2 `domain/wallet/exception/WalletCurrencyMismatchException.java`

**Caminho:** `.../domain/wallet/exception/WalletCurrencyMismatchException.java`

```java
package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

public class WalletCurrencyMismatchException extends RuntimeException {

    public WalletCurrencyMismatchException(String message) {
        super(message);
    }
}
```

> **Justificativa de naming:** o projeto já tem
> `payment.CurrencyMismatchException`. Não reaproveitar (e não promover
> para `shared`) por ora — o pacote `payment` é específico de pagamento.
> Promoção a `domain.shared` é um refactor independente e não deve
> entrar no escopo deste PR.

### 4.3 `domain/wallet/policy/BulletAllocationPolicy.java`

**Caminho:** `.../domain/wallet/policy/BulletAllocationPolicy.java`

```java
package br.com.casellisoftware.budgetmanager.domain.wallet.policy;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;

import java.util.Objects;

/**
 * Invariantes para alocar (ou desalocar) orçamento de uma {@link Wallet}
 * para um bullet. Espelha {@code PaymentPolicy} em estrutura.
 *
 * <p>Não tem estado, não conhece repositórios — pura função de
 * domínio. Toda quebra é convertida em exceção dedicada para
 * preservar mensagens claras na borda.</p>
 */
public final class BulletAllocationPolicy {

    private BulletAllocationPolicy() {
    }

    /**
     * Valida que {@code requestedBudget} cabe no {@code remaining} de
     * {@code wallet} e que ambos compartilham a mesma moeda.
     */
    public static void validateAllocation(Wallet wallet, Money requestedBudget) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(requestedBudget, "requestedBudget must not be null");

        requireSameCurrency(wallet, requestedBudget);

        if (requestedBudget.isGreaterThan(wallet.getRemaining())) {
            throw new WalletAllocationExceededException(
                    "Requested bullet budget " + requestedBudget.amount()
                            + " exceeds wallet remaining " + wallet.getRemaining().amount()
                            + " (walletId=" + wallet.getId() + ")");
        }
    }

    /**
     * Valida uma realocação de orçamento (patch). Recebe o delta
     * efetivo: positivo significa "preciso de mais orçamento da
     * wallet"; negativo significa "estou devolvendo".
     */
    public static void validateReallocation(Wallet wallet, Money currentBudget, Money newBudget, Money currentBulletRemaining) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(currentBudget, "currentBudget must not be null");
        Objects.requireNonNull(newBudget, "newBudget must not be null");
        Objects.requireNonNull(currentBulletRemaining, "currentBulletRemaining must not be null");

        requireSameCurrency(wallet, newBudget);

        // O orçamento já consumido (pago) é intocável.
        Money consumed = currentBudget.subtract(currentBulletRemaining);
        if (consumed.isGreaterThan(newBudget)) {
            throw new WalletAllocationExceededException(
                    "New bullet budget " + newBudget.amount()
                            + " is below already consumed amount " + consumed.amount());
        }

        // Se houver aumento, ele precisa caber no remaining da wallet.
        if (newBudget.isGreaterThan(currentBudget)) {
            Money delta = newBudget.subtract(currentBudget);
            if (delta.isGreaterThan(wallet.getRemaining())) {
                throw new WalletAllocationExceededException(
                        "Bullet budget increase of " + delta.amount()
                                + " exceeds wallet remaining " + wallet.getRemaining().amount());
            }
        }
    }

    private static void requireSameCurrency(Wallet wallet, Money money) {
        if (!wallet.getBudget().currency().equals(money.currency())) {
            throw new WalletCurrencyMismatchException(
                    "Currency mismatch: wallet=" + wallet.getBudget().currency()
                            + " requested=" + money.currency());
        }
    }
}
```

### 4.4 Ajuste em `domain/wallet/Wallet.java`

Adicionar um método `credit(Money)` simétrico a `debit(Money)`. Manter
imutabilidade.

```java
/**
 * Returns a new {@code Wallet} with {@code remaining} increased by {@code amount}.
 * Used to refund the wallet when a bullet is deleted or has its budget reduced.
 */
public Wallet credit(Money amount) {
    Money newRemaining = this.remaining.add(amount);
    if (newRemaining.isGreaterThan(this.budget)) {
        throw new IllegalStateException(
                "credit would push remaining above budget: id=" + this.id);
    }
    return new Wallet(this.id, this.description, this.budget, newRemaining,
            this.startDate, this.closedDate, this.closed);
}
```

> A guarda `newRemaining > budget` protege contra credit-com-bug que
> levaria a wallet a um estado fisicamente impossível. É uma invariante
> e portanto mora no domínio.

### 4.5 (Opcional) Helper em `Bullet.java`

Para o patch ficar legível na use case:

```java
public Money consumed() {
    return this.budget.subtract(this.remaining);
}
```

Não é estritamente necessário (a policy faz a subtração), mas torna
testes e logs mais expressivos. Inclui se o diff ficar limpo.

---

## 5. Application layer — código

### 5.1 Novo boundary `FindWalletDomainByIdBoundary`

**Caminho:**
`application/src/main/java/br/com/casellisoftware/budgetmanager/application/wallet/boundary/FindWalletDomainByIdBoundary.java`

```java
package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

/**
 * Retorna o agregado {@link Wallet} (não o DTO). Existe especificamente
 * para use cases que precisam mutar o estado da wallet — diferente de
 * {@link FindWalletByIdBoundary}, que serve a borda REST.
 *
 * @throws br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException
 *         quando o id não existe
 */
public interface FindWalletDomainByIdBoundary {

    Wallet findById(String id);
}
```

### 5.2 Use case correspondente

**Caminho:**
`.../application/wallet/usecase/FindWalletDomainByIdUseCase.java`

```java
package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;

public class FindWalletDomainByIdUseCase implements FindWalletDomainByIdBoundary {

    private final WalletRepository walletRepository;

    public FindWalletDomainByIdUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet findById(String id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }
}
```

### 5.3 Refactor de `SaveBulletUseCase`

**Caminho:** `.../application/bullet/usecase/SaveBulletUseCase.java`
(arquivo existente).

```java
package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.BulletAllocationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveBulletUseCase implements SaveBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public SaveBulletUseCase(BulletRepository bulletRepository,
                             WalletRepository walletRepository,
                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public BulletOutput execute(BulletInput input) {
        log.info("Saving bullet for walletId={}", input.walletId());

        Wallet wallet = findWalletDomainByIdBoundary.findById(input.walletId());
        Money budget = Money.of(input.budget());

        BulletAllocationPolicy.validateAllocation(wallet, budget);

        Wallet debited = wallet.debit(budget);
        Bullet bullet = Bullet.create(input.description(), budget, budget, wallet.getId());

        walletRepository.save(debited);                 // (1) compromete o orçamento
        Bullet saved = bulletRepository.save(bullet);   // (2) materializa o envelope

        log.info("Bullet saved id={} walletRemaining={}", saved.getId(), debited.getRemaining().amount());
        return BulletOutputAssembler.from(saved);
    }
}
```

> **Atenção à regra dourada #4.** A use case continua dependendo de
> `WalletRepository` (porta de domínio, OK) e do boundary
> `FindWalletDomainByIdBoundary` (porta de application, OK). Nenhum
> `*UseCase` concreto é injetado.

### 5.4 Refactor de `PatchBulletUseCase`

Cobrir três cenários no patch:
- `budget` não muda → comportamento atual.
- `budget` aumenta → debitar a wallet pelo delta.
- `budget` diminui → creditar a wallet pelo delta. **Trava** se o novo
  budget for menor que o já consumido (`oldBudget - oldRemaining`).

```java
package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInputAssembler;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.BulletAllocationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchBulletUseCase implements PatchBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public PatchBulletUseCase(BulletRepository bulletRepository,
                              WalletRepository walletRepository,
                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public BulletOutput execute(PatchBulletInput input) {
        log.info("Patching bullet id={}", input.id());

        Bullet existing = bulletRepository.findById(input.id())
                .orElseThrow(() -> new BulletNotFoundException(input.id()));

        BulletPatch patch = PatchBulletInputAssembler.toPatch(input);
        Bullet patched = existing.patch(patch);

        boolean budgetChanged = !existing.getBudget().equals(patched.getBudget());
        if (budgetChanged) {
            reconcileWallet(existing, patched);
        }

        Bullet saved = bulletRepository.save(patched);
        log.info("Bullet patched successfully, id={}", saved.getId());
        return BulletOutputAssembler.from(saved);
    }

    private void reconcileWallet(Bullet existing, Bullet patched) {
        Wallet wallet = findWalletDomainByIdBoundary.findById(existing.getWalletId());

        BulletAllocationPolicy.validateReallocation(
                wallet,
                existing.getBudget(),
                patched.getBudget(),
                existing.getRemaining()
        );

        Wallet reconciled;
        if (patched.getBudget().isGreaterThan(existing.getBudget())) {
            Money delta = patched.getBudget().subtract(existing.getBudget());
            reconciled = wallet.debit(delta);
        } else {
            Money delta = existing.getBudget().subtract(patched.getBudget());
            reconciled = wallet.credit(delta);
        }

        walletRepository.save(reconciled);
    }
}
```

> **Edge case explícito:** se o patch também alterar `remaining` do
> bullet (o `BulletPatch` permite), a use case **não** propaga essa
> mudança para a wallet. Mexer em `bullet.remaining` representa um
> ajuste contábil interno (ex.: corrigir um pagamento) e o impacto
> correto é no agregado de pagamento, não na wallet. Documentar isso
> no Javadoc da use case.

### 5.5 Novo `DeleteBulletByIdBoundary` + `DeleteBulletByIdUseCase`

Hoje **não existe** delete de bullet. O fluxo de "delete expense" zera
pagamentos e devolve o `remaining` aos bullets via patch — mas o bullet
em si permanece. Ao introduzir delete:

- Bloqueia delete se houver pagamentos atrelados ao bullet
  (`Payment.bulletId == bullet.id`). Razão: o bullet manter integridade
  histórica de auditoria.
- Devolve `bullet.remaining` à wallet (regra de ouro #7).

**Caminho do boundary:**
`.../application/bullet/boundary/DeleteBulletByIdBoundary.java`

```java
package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

public interface DeleteBulletByIdBoundary {

    void execute(String id);
}
```

**Use case:** `.../application/bullet/usecase/DeleteBulletByIdUseCase.java`

```java
package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteBulletByIdUseCase implements DeleteBulletByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteBulletByIdUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public DeleteBulletByIdUseCase(BulletRepository bulletRepository,
                                   WalletRepository walletRepository,
                                   PaymentRepository paymentRepository,
                                   FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public void execute(String id) {
        log.info("Deleting bullet id={}", id);

        Bullet bullet = bulletRepository.findById(id)
                .orElseThrow(() -> new BulletNotFoundException(id));

        if (paymentRepository.existsByBulletId(id)) {
            throw new BulletInUseException(id);
        }

        Wallet wallet = findWalletDomainByIdBoundary.findById(bullet.getWalletId());
        Wallet refunded = wallet.credit(bullet.getRemaining());

        walletRepository.save(refunded);   // (1)
        bulletRepository.deleteById(id);   // (2)

        log.info("Bullet deleted id={} refundedToWallet={}", id, bullet.getRemaining().amount());
    }
}
```

> Esse use case implica três adições menores no domain (precisam ser
> rastreadas nas tarefas):
> - `BulletInUseException` em `domain/bullet/`.
> - `PaymentRepository#existsByBulletId(String)` (port + impl).
> - `BulletRepository#deleteById(String)` (port + impl, se ainda
>   não existir).

---

## 6. Infra layer — decoradores transacionais

### 6.1 Justificativa da ordem `(1) Wallet → (2) Bullet`

Dentro de uma transação Mongo, a ordem só importa para
"o que persiste em caso de panic *antes* do commit". Sem transação
seria diferente; com transação multi-document Mongo, ambas as escritas
ou comitam ou voltam atrás. Escolher Wallet primeiro tem dois
benefícios menores:

- Em logs, a operação que **compromete** orçamento aparece antes — útil
  pra debugar.
- Se um dia rodarmos com `causalConsistency=false` ou em ambiente
  sem transação (testes), a falha do bullet deixa o estado pior
  (orçamento gasto sem entrega) — o que **deve** chamar a atenção. O
  reverso (bullet sem wallet debitada) seria silencioso e perigoso.

### 6.2 `TransactionalSaveBulletBoundary`

**Caminho:**
`infra/src/main/java/br/com/casellisoftware/budgetmanager/configs/transactional/TransactionalSaveBulletBoundary.java`

```java
package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveBulletBoundary implements SaveBulletBoundary {

    private final SaveBulletBoundary delegate;

    public TransactionalSaveBulletBoundary(SaveBulletBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public BulletOutput execute(BulletInput input) {
        return delegate.execute(input);
    }
}
```

### 6.3 `TransactionalPatchBulletBoundary`

**Caminho:** `.../configs/transactional/TransactionalPatchBulletBoundary.java`

```java
package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPatchBulletBoundary implements PatchBulletBoundary {

    private final PatchBulletBoundary delegate;

    public TransactionalPatchBulletBoundary(PatchBulletBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public BulletOutput execute(PatchBulletInput input) {
        return delegate.execute(input);
    }
}
```

### 6.4 `TransactionalDeleteBulletByIdBoundary`

```java
package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteBulletByIdBoundary implements DeleteBulletByIdBoundary {

    private final DeleteBulletByIdBoundary delegate;

    public TransactionalDeleteBulletByIdBoundary(DeleteBulletByIdBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id) {
        delegate.execute(id);
    }
}
```

> **Importante:** o `DeleteExpenseByIdUseCase` chama
> `PatchBulletBoundary` internamente. Ao publicar
> `TransactionalPatchBulletBoundary` como bean exposto pelo tipo
> `PatchBulletBoundary`, o delete-expense passará a abrir uma transação
> aninhada por bullet refundado. Isso é seguro com `Propagation.REQUIRED`
> (default) — entra na tx existente do `TransactionalDeleteExpenseBoundary`.
> **Validar isso explicitamente em teste de integração** (item §10.4).

### 6.5 Atualização de `BusinessLayerBeanConfiguration`

Trocar os `@Bean`s de save/patch/delete de bullet:

```java
@Bean
public FindWalletDomainByIdBoundary findWalletDomainByIdBoundary(WalletRepository walletRepository) {
    return new FindWalletDomainByIdUseCase(walletRepository);
}

@Bean
public SaveBulletBoundary saveBulletBoundary(BulletRepository bulletRepository,
                                             WalletRepository walletRepository,
                                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
    SaveBulletUseCase useCase = new SaveBulletUseCase(
            bulletRepository, walletRepository, findWalletDomainByIdBoundary);
    return new TransactionalSaveBulletBoundary(useCase);
}

@Bean
public PatchBulletBoundary patchBulletBoundary(BulletRepository bulletRepository,
                                               WalletRepository walletRepository,
                                               FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
    PatchBulletUseCase useCase = new PatchBulletUseCase(
            bulletRepository, walletRepository, findWalletDomainByIdBoundary);
    return new TransactionalPatchBulletBoundary(useCase);
}

@Bean
public DeleteBulletByIdBoundary deleteBulletByIdBoundary(BulletRepository bulletRepository,
                                                         WalletRepository walletRepository,
                                                         PaymentRepository paymentRepository,
                                                         FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
    DeleteBulletByIdUseCase useCase = new DeleteBulletByIdUseCase(
            bulletRepository, walletRepository, paymentRepository, findWalletDomainByIdBoundary);
    return new TransactionalDeleteBulletByIdBoundary(useCase);
}
```

> **Remover** os `@Bean public SaveBulletUseCase ...` e
> `@Bean public PatchBulletUseCase ...` antigos. Preservar o
> `FindBulletByIdUseCase` e o `FindAllBulletsByIdsUseCase` como estão.

---

## 7. REST layer

### 7.1 `BulletController`

Adicionar `PATCH` e `DELETE` (se ainda não tiver). Hoje só existem
`POST` e `GET /{id}`.

```java
@PatchMapping("/{id}")
public ResponseEntity<BulletResponseDto> patch(@PathVariable String id,
                                               @Valid @RequestBody BulletPatchRequestDto request) {
    BulletOutput output = patchBulletBoundary.execute(
            mapper.bulletPatchRequestDtoToInput(id, request)
    );
    return ResponseEntity.ok(mapper.bulletOutputToBulletResponseDto(output));
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable String id) {
    deleteBulletByIdBoundary.execute(id);
    return ResponseEntity.noContent().build();
}
```

(Os DTOs de patch já existem para o resto do app — espelhar
`Wallet`/`Expense`. Se ainda não houver `BulletPatchRequestDto`, criar
seguindo o template existente em `rest.expense.dtos`.)

### 7.2 `GlobalExceptionHandler`

Mapear as exceções novas:

| Exceção | HTTP |
|---|---|
| `WalletAllocationExceededException` | `409 CONFLICT` |
| `WalletCurrencyMismatchException` | `422 UNPROCESSABLE_ENTITY` |
| `BulletInUseException` | `409 CONFLICT` |

Justificativa do `409` para "exceeded": a requisição é sintaticamente
válida, mas conflita com o estado atual do recurso (saldo).

---

## 8. Análise Big-O

| Operação | Reads | Writes | Total |
|---|---|---|---|
| `SaveBullet` | 1 (wallet) | 2 (wallet, bullet) | **O(1)** |
| `PatchBullet` (sem mudar budget) | 1 (bullet) | 1 (bullet) | **O(1)** |
| `PatchBullet` (mudando budget) | 2 (bullet, wallet) | 2 (bullet, wallet) | **O(1)** |
| `DeleteBullet` | 2 (bullet, wallet) + 1 (existsByBulletId) | 1 write + 1 delete | **O(1)** |
| `GET /wallets/{id}` | 1 | 0 | **O(1)** |

`existsByBulletId` deve cair num índice em `payments.bulletId`
(`@Indexed`). Sem índice, é collection scan **O(p)** com p =
pagamentos da coleção. Veja §11.

---

## 9. SOLID — checklist por arquivo novo/alterado

- **SRP:** `SaveBulletUseCase`, `PatchBulletUseCase`,
  `DeleteBulletByIdUseCase` orquestram; `BulletAllocationPolicy`
  guarda invariantes; `Transactional<...>Boundary` carrega só a
  preocupação transacional.
- **OCP:** novas regras de alocação se acrescentam à policy sem
  mudar use case. Decoradores adicionam comportamento sem alterar a
  use case original (decorator pattern).
- **LSP:** decoradores e use cases implementam o mesmo boundary;
  semântica idêntica do ponto de vista do controller.
- **ISP:** `FindWalletDomainByIdBoundary` é separado de
  `FindWalletByIdBoundary` justamente para não inflar uma interface
  com dois retornos (entidade vs DTO).
- **DIP:** use cases dependem só de portas (`*Repository`,
  `*Boundary`); decoradores e config de infra cabem na implementação.

---

## 10. Plano de testes

### 10.1 Domain — `BulletAllocationPolicyTest`

Casos:
- happy path: `requestedBudget == wallet.remaining` → não lança;
- exceeded: `requestedBudget > wallet.remaining` →
  `WalletAllocationExceededException`;
- currency mismatch → `WalletCurrencyMismatchException`;
- `validateReallocation`: aumento que cabe; aumento que não cabe;
  redução que respeita o consumido; redução que viola o consumido;
  mismatch de moeda.

### 10.2 Domain — `WalletTest#credit`

- credit dentro do budget: novo `remaining` = anterior + amount.
- credit que ultrapassa budget → `IllegalStateException`.
- credit imutável: instância original intacta.

### 10.3 Application — atualizar `SaveBulletUseCaseTest`

Casos novos / atualizados:
- happy path: verifica `walletRepository.save(walletDebitado)` **antes**
  de `bulletRepository.save(bullet)` (`InOrder`).
- wallet not found: `BulletRepository` não é tocado.
- exceeded: a exceção da policy é propagada e nem wallet nem bullet
  são persistidos.
- currency mismatch: idem.
- repository (wallet) falha: bullet não é persistido. (Sem o
  decorator, esse teste prova só a chamada; o teste de integração
  prova o rollback.)

### 10.4 Integração — transação Mongo

Usar `@DataMongoTest` + `MongoDB Testcontainers` (replica set, exigência
para multi-document tx). Cobrir:
- save bullet com falha forçada na segunda escrita → wallet volta ao
  estado anterior.
- delete-expense que dispara patch-bullet aninhado → uma única tx,
  rollback global se algo falhar no meio.

### 10.5 REST — `BulletControllerTest`

- `POST /bullets` com `budget > wallet.remaining` → `409`.
- `POST /bullets` com moeda diferente → `422`.
- `PATCH /bullets/{id}` reduzindo budget abaixo do consumido → `409`.
- `DELETE /bullets/{id}` em bullet com pagamentos → `409`.

---

## 11. Riscos e mitigações

| Risco | Probabilidade | Mitigação |
|---|---|---|
| Race condition em saves concorrentes da mesma wallet | Média | `@Version` em `WalletDocument` (próximo PR de robustez do raio-x). Por ora, transação Mongo cobre consistência intra-operação. |
| `existsByBulletId` sem índice | Alta sem ação | Adicionar `@Indexed` em `PaymentDocument#bulletId`. |
| Drift histórico (wallets que já têm bullets sem o débito correspondente) | Alta na primeira release | Script de migração one-shot em `infra/migration/` que recalcula `wallet.remaining = wallet.budget - Σ bullet.budget`. Documentar em CHANGELOG. |
| Testes Mongo lentos | Média | Manter testes de integração separados (`*IT.java`) com profile dedicado. |
| Confusão entre `FindWalletByIdBoundary` e `FindWalletDomainByIdBoundary` | Baixa | Javadoc em ambos explicitando o público-alvo. |

---

## 12. Sequência sugerida de PRs

1. **PR-1 (domain only, zero risco em produção):**
   `Money` se mantém, `Wallet.credit`, `BulletAllocationPolicy`,
   exceções, testes de domínio. Não muda nenhum fluxo.
2. **PR-2 (application + infra, sem migração ainda):**
   novo boundary/use case de `FindWalletDomainByIdBoundary`,
   `SaveBulletUseCase` reescrito, `TransactionalSaveBulletBoundary`,
   atualização do `BusinessLayerBeanConfiguration`. **Atenção:** ao
   liberar este PR, novos bullets passam a debitar a wallet. Os bullets
   antigos ainda não estarão refletidos no `wallet.remaining` →
   ver PR-3.
3. **PR-3 (migração one-shot):** script que percorre wallets e recalcula
   `remaining` com base nos bullets existentes. Idempotente.
4. **PR-4 (patch + delete):** `PatchBulletUseCase` reescrito,
   `DeleteBulletByIdBoundary`/UseCase, decoradores, controller, REST,
   handler, testes. `PaymentRepository#existsByBulletId` + índice.
5. **PR-5 (testes de integração transacional):** Testcontainers,
   asserts de rollback.

---

## 13. Definition of Done (todos os checkbox precisam ficar marcados)

- [ ] Domain compila sem dependência de Spring (PR-1).
- [ ] `BulletAllocationPolicy` 100% coberto por testes unitários.
- [ ] `SaveBulletUseCase` testes cobrem happy path, exceeded, mismatch,
      wallet-not-found, falha de repositório.
- [ ] Decoradores transacionais expostos como `@Bean` do tipo *boundary*
      (não da classe concreta da use case).
- [ ] Nenhum `*UseCase` concreto injetado em outra use case (DIP).
- [ ] `Wallet.credit` jamais permite `remaining > budget`.
- [ ] `PaymentDocument.bulletId` tem `@Indexed`.
- [ ] Script de migração rodado em ambiente de staging com saldo
      de wallets validado manualmente em ao menos 5 casos.
- [ ] `wallet.remaining` permanece O(1) em todas as leituras.
- [ ] `RAIO-X_ARQUITETURAL_20260414.md` atualizado: alocação de bullet
      passa a estar coberta; itens 4 e 5 do raio-x ganham mais um
      precedente correto.
