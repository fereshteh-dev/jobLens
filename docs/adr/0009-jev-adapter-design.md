# ADR-0009: Jev adapter design choices

Date: 2026-09-21

## Context

Phase 4 built the real `JevPostingClassifier`. A handful of decisions weren't fully specified
by the brief and are worth recording, plus one thing the brief's own domain content implied
but didn't state outright.

## Decisions

- **`state` includes the title, company and location, not just the description.** The
  `title_vs_reality` question asks Jev to compare the title to the described responsibilities
  - it can't do that if the title was never sent. `JobPosting.title()` isn't otherwise part of
  any question's `state`, so this would have been a real bug (an unanswerable question) if
  `state` were just `descriptionText()`.
- **Claimed seniority is parsed from the title locally (`ClaimedSeniorityParser`), not asked
  to Jev.** The brief's Jev question list only defines `actual_seniority` and
  `title_vs_reality` - no `claimed_seniority` question - so the claim has to come from
  somewhere else. A simple keyword parse is cheap, deterministic, and testable without Jev.
  Extracted into `analysis.domain` so `MockPostingClassifier` and `JevPostingClassifier` share
  it instead of duplicating the same heuristic (Phase 2's mock had its own private copy;
  removed once this landed).
- **`JevResponse.JevAnswer` is one flattened record for all three answer shapes** (choice/
  score/noul), not a polymorphic sealed hierarchy - a `noul` answer just leaves `choice`/
  `score`/etc. null. Simpler, and Jackson polymorphic type handling didn't need verifying
  for Jackson 3 as a result. This app never uses `score` questions, so no score-specific
  mapping exists; add it if the schema ever needs one.
- **`Map<String, Object>` requests, not typed request DTOs.** `JevPostingClassifier` builds
  the `questions` payload as plain maps from `JevQuestionSchema`, rather than modeling
  choice/noul as separate Jackson-serializable types. Avoids needing a discriminator/
  polymorphism story for something that's write-only from this app's side.
- **`MockRestServiceServer` instead of a new mock-HTTP-server dependency (e.g. WireMock).**
  It's Spring's own built-in mechanism for exactly this - testing `RestClient`-based code
  against expected request/response pairs - and was already available via
  `spring-boot-starter-test`. Matches "prefer built-ins" from the brief; no new dependency
  needed for `JevPostingClassifierTest`.
- **Retry differs from the LLM adapter's**: exponential backoff (`IntervalFunction.ofExponentialBackoff`),
  per Jev's own documented guidance ("retry with exponential backoff instead of retrying
  immediately"), and a narrower `retryOnException` predicate - only network/timeout,
  5xx (including Jev's non-standard 529 "overloaded", which Spring classifies as a
  `HttpServerErrorException` since it checks the numeric 5xx range, not a fixed enum), and
  429. A 401 (bad key) or 422 (bad request) is not retried - retrying either just wastes
  attempts. The LLM adapter's simpler fixed-wait, retry-on-anything policy from Phase 3 was
  left as-is rather than generalized to match, since the two providers gave genuinely
  different guidance; a shared resilience factory was considered and rejected for this reason
  (see the code comment in `JevPostingClassifier` and `SpringAiTalkingPointsWriter`).

## Consequences

- If Jev ever adds a `score` question to the schema, `JevPostingClassifier.toAnalysis()` needs
  a new mapping branch - nothing currently reads `JevAnswer.score()`/`legend()`.
- Verified live against an unreachable `joblens.jev.base-url`: the app returns a `502`
  Problem Detail ("Classification service unavailable") after retries/circuit breaker are
  exhausted, and nothing in the logs includes posting text - matches CLAUDE.md and
  ADR-0003's "if Jev is down, return a clear error" rule.
