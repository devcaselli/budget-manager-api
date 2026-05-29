---
name: security-infrastructure
description: JWT self-signed RSA setup, BCrypt passwords, conditional security config for tests
metadata:
  type: project
---

Auth was migrated from Keycloak (OIDC) to self-signed RSA JWT:
- `JwtKeyConfiguration` — loads RSA keys from `app.jwt.private-key` / `app.jwt.public-key` PEM env vars.
- `JwtTokenGenerator` — `@Component` implementing `TokenGeneratorPort`; uses RS256, sets `aud` = issuer.
- `BCryptPasswordEncoderAdapter` — `@Component` implementing `PasswordEncoderPort`; strength=12.
- `SecurityConfiguration` uses `NimbusJwtDecoder.withPublicKey(rsaPublicKey)`, not jwk-set-uri.
- `TestPermitAllSecurityConfiguration` at `infra/src/main/java/.../configs/security/` — activated when `app.security.enabled=false`, permits all without auth. NOT in test sources.
- Public endpoints: `/auth/register`, `/auth/token` added to permitAll in `SecurityConfiguration`.

Keys are provided via env vars (`.env` file in dev). `.env.example` at project root documents the format.

`UserNotFoundException` maps to **401 Unauthorized** (not 404) in `GlobalExceptionHandler` to avoid info leak about whether an email exists.
`UserAlreadyExistsException` maps to **409 Conflict**.
