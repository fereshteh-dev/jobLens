# ADR-0005: Resilience and rate limiting

Date: 2026-09-21

## Context

Brief requires timeouts everywhere, retry only for idempotent transient failures, a circuit
breaker for Jev and the LLM, and per-user rate limiting — preferring Spring Framework 7
built-ins over libraries where they suffice.

## Decision

- **Timeouts**: Spring Boot 4's global `spring.http.clients.connect-timeout` /
  `spring.http.clients.read-timeout` properties, which apply to all auto-configured
  `RestClient`/`RestTemplate`/`WebClient` builders (confirmed against the Boot 4.1
  reference docs). Built-in, no library needed. Not independently verified against a live
  LLM call in this phase (no API key available in this environment) — worth a manual
  check once a real key is configured (see ADR-0011 for a live-testing session that
  surfaced a related bug, though not this specific timeout question).
- **Retry + circuit breaker**: Resilience4j only (`resilience4j-spring-boot3`, `2.4.0`),
  dropping the originally-planned separate Spring Retry dependency — using two different
  retry mechanisms on the same call path (Spring Retry's `@Retryable` stacked with
  Resilience4j's own `@Retry`) would just be two libraries doing the same job and risking
  double-retry confusion. Resilience4j's `@Retry` and `@CircuitBreaker` annotations compose
  cleanly on the same method with a shared `fallbackMethod`. Compatibility with Boot
  4.1.1/Framework 7 (flagged as unconfirmed in ADR-0001) is checked empirically in this
  phase: both annotations are applied to `SpringAiTalkingPointsWriter`, and the full
  `@SpringBootTest` context must load successfully for the phase to be considered done.
- **Rate limiting**: Bucket4j (`8.10.1`, package `io.github.bucket4j` despite the `com.bucket4j`
  Maven coordinates — worth remembering), one in-memory bucket per rate-limit key. Since
  real API-key authentication doesn't exist yet (planned for Phase 5, alongside the
  extension), the key is `X-API-Key` header value when present, else the client's remote
  address. This means the limiter is bypassable today by rotating the header value; it's real
  infrastructure, not real protection, until Phase 5 adds actual key validation. Flagged
  explicitly rather than silently left as a gap.

## Consequences

- The LLM adapter (`SpringAiTalkingPointsWriter`) gets `@Retry` + `@CircuitBreaker` with a
  shared fallback that throws `TalkingPointsUnavailableException`; `AnalyzePostingUseCase`
  catches exactly that type and degrades to `AnalysisOutcome.WithoutTalkingPoints` - never a
  generic catch-all. `PostingClassifier` (Jev, Phase 4) gets the identical annotation
  pattern once it exists; a classifier failure is deliberately *not* caught anywhere and
  propagates to the generic 500 handler, per the brief's "if Jev is down, return a clear
  error" rule.
- API-key **authentication** (validating the header, not just using it as a rate-limit
  bucket key), CORS, and request size limits are still not built. They're deferred to
  Phase 5, when the extension exists and there's a real client to authenticate against -
  flagged here rather than silently dropped.
