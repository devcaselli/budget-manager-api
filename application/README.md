# application module — Bean Wiring Rule

This module is **zero-Spring**: no `@Component`, `@Service`, `@Autowired`, or any
`org.springframework` import is allowed in production code.

## How beans are registered

Every public class in `application/` that needs to be a Spring bean (use cases,
application services) is registered explicitly via `@Bean` methods in a
`@Configuration` class inside the `infra/` module
(e.g. `BusinessLayerBeanConfiguration`).

Records, value objects, utility classes, and interfaces are not beans and do not
need registration.

## Why

Keeping `application/` framework-free ensures the dependency arrow always points
inward (infra → application → domain). The application layer stays testable with
plain unit tests — no Spring context required.
