# ADR-0001: Version baseline

Date: 2026-09-21

## Context

The brief bans milestones/RCs/snapshots and forbids trusting memory for versions. Verified
against official sources on 2026-09-21.

## Decision

| Component | Version | Source |
|---|---|---|
| Java | 25 LTS | start.spring.io metadata (Java options: 17/21/25/27) |
| Spring Boot | 4.1.1 | `maven-metadata.xml` for `spring-boot-starter-parent`, `<release>` |
| Spring Framework | 7.0.x | Spring Boot 4.0.0 GitHub release notes ("Upgrade to Spring Framework 7.0.1"); exact patch resolved transitively by the Boot 4.1.1 BOM |
| Jackson | 3.0.2 | Spring Boot 4.0.0 release notes ("Upgrade to Jackson Bom 3.0.2"); package root moves to `tools.jackson.*`, `jackson-annotations` stays `com.fasterxml.jackson.annotation` (depends on Jackson 2.20 for that module only) |
| Spring AI | 2.0.1 | `maven-metadata.xml` for `spring-ai-bom`, `<release>`; GitHub release notes reference upgrading to Spring Boot 4.1.0 |
| Node.js | 24 ("Krypton") | nodejs.org previous-releases table, current Active LTS |
| Vite | 8.3.0 | `registry.npmjs.org/vite/latest` |
| TypeScript | 7.0.2 | `registry.npmjs.org/typescript/latest` |
| Vitest | 5.0.1 | `registry.npmjs.org/vitest/latest` |
| ArchUnit | 1.5.0 | `maven-metadata.xml` for `archunit-junit5` |
| Spotless (Maven plugin) | 3.10.2 | `maven-metadata.xml` |
| Caffeine | 3.3.0 | `maven-metadata.xml` |
| springdoc-openapi | 3.1.1 | `maven-metadata.xml` for `springdoc-openapi-starter-webmvc-ui` |
| Resilience4j (Spring Boot integration) | 2.4.0 | `maven-metadata.xml` for `resilience4j-spring-boot3`; **compatibility with Boot 4.1/Framework 7 not yet confirmed, re-verify in Phase 3** |
| Bucket4j | 8.10.1 | `maven-metadata.xml` for `bucket4j-core` |

## Jev API (verified against docs.typesafe.ai)

- `POST https://api.typesafe.ai/v1/systemone`, header `Authorization: Bearer <key>`.
- Body: `{ state, model: "jev-latest", questions: { <id>: {...} } }`.
- Question types: `choice` (`instructions` + `criteria` map → `choice`, `confidence`,
  `probabilities`), `score` (`instructions` + ordered `criteria` array, 2-10 levels →
  `score`, `legend`, `probabilities`, `confidence`), `noul` (`instructions` only → `noul`
  probability, 0-1).
- Response: `{ model, answers: { ...typed per question... }, usage: { input_tokens,
  output_tokens } }`. `model` reports the exact resolved version (e.g. `jev-1.13.0`) — logged
  per call as required by the brief.
- No official Java SDK confirmed (Python and JavaScript SDKs exist) — a small `RestClient`
  wrapper is the right call, matching the brief.

## Consequences

- Because Boot 4 / Framework 7 / Jackson 3 are very recent, some third-party libraries
  (Resilience4j's Spring integration, springdoc) may lag. Each is re-verified at the start of
  the phase that first needs it rather than blocking Phase 0/1.
- Jackson 3's package rename means any hand-written Jackson code (if ever needed outside
  Spring's auto-configuration) uses `tools.jackson.*`, not `com.fasterxml.jackson.*`.

## Update from Phase 2 (2026-09-21): Boot 4's test starters are modularized too

`spring-boot-starter-test` no longer pulls in MockMvc/`@WebMvcTest` support - that moved to
a separate `spring-boot-starter-webmvc-test` artifact, and the annotations themselves moved
package from `org.springframework.boot.test.autoconfigure.web.servlet` to
`org.springframework.boot.webmvc.test.autoconfigure`. Found by actually running the build,
not by re-reading docs first - worth remembering as a pattern for Phase 3/4: Boot 4's modular
starters (mentioned in passing in ADR-0001's original version table) extend to test
infrastructure too, so don't assume a Boot 3-era starter still pulls in everything it used to.

## Update from Phase 3 (2026-09-21): another removed starter

`spring-boot-starter-aop` (needed for Resilience4j's `@Retry`/`@CircuitBreaker` aspects) no
longer exists as an artifact in the Boot 4.1.1 BOM at all - confirmed by fetching the BOM's
own POM. The BOM manages `org.aspectj:aspectjweaver` directly instead; adding that dependency
alone (letting Boot pick the version) is what triggers `AopAutoConfiguration`. Second
confirmation of the pattern above: check every Boot-3-era starter name against the actual
4.1.1 BOM before assuming it still exists, rather than trusting memory of Boot 3.

## Update from Phase 4 (2026-09-21): a third removed-from-web-starter surprise

`RestClient.Builder` auto-configuration (needed for `JevHttpClient`) is no longer part of
`spring-boot-starter-web` either - it moved to a separate `spring-boot-starter-restclient`
module (confirmed by the local Maven repo layout: `spring-boot-http-client`,
`spring-boot-http-codec`, `spring-boot-http-converter`, `spring-boot-restclient` are all now
distinct artifacts). Third instance of the same pattern (after `@WebMvcTest`'s move in Phase 2
and `spring-boot-starter-aop`'s removal in Phase 3): Boot 4's `spring-boot-starter-web` is much
thinner than its Boot 3 equivalent. Treat this as the default expectation for Phase 6 (CI,
springdoc) rather than a one-off.
