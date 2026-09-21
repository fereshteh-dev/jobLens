# ADR-0002: Hexagonal architecture, package-by-feature, `profile` as a sibling package

Date: 2026-09-21

## Context

The brief mandates ports-and-adapters with domain/application ignorant of Spring, Jackson,
HTTP and provider types, plus a package-by-feature layout.

## Decision

Keep the suggested structure almost as-is (it's already right for a single-feature app):
`analysis/{domain,application,adapter}` and a separate top-level `profile/` package for
`CandidateProfile`. `GatePolicy` (in `analysis.domain`) takes `CandidateProfile` as a plain
method parameter — it does not depend on `profile.adapter`, only on the `profile.domain`
type. Application-layer wiring is what actually loads the profile and hands it to the use
case; the domain never knows the loading mechanism (YAML file, env var, etc.).

`RedFlagKind` is a standalone enum, one entry per Jev `noul` question, rather than being
implicit in six boolean fields — keeps `RedFlag` a two-field record and keeps the mapping
from Jev question id → domain concept explicit and exhaustive (a `switch` over `RedFlagKind`
with no default branch will fail to compile if a question is ever added and forgotten).

## Consequences

- ArchUnit rule set: `analysis.domain` and `analysis.application` forbidden from depending on
  `org.springframework..`, `com.fasterxml.jackson..` / `tools.jackson..`, `java.net.http..`,
  or any `analysis.adapter..` / `profile.adapter..` package. `profile.domain` is allowed as a
  dependency of `analysis.domain` (both are pure).
- If a second feature is ever added (out of scope today), it gets its own
  `<feature>/{domain,application,adapter}` tree; `profile` stays shared because both features
  would need the candidate profile.
