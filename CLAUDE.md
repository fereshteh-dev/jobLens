# JobLens

Spring Boot backend + thin Chrome MV3 extension that analyzes a job posting and returns
real-vs-claimed seniority, red flags, and visa/relocation likelihood. Only postings that
pass a configurable gate get LLM-generated talking points.

## Non-negotiable architecture rule

- **Jev (TypeSafe AI System One) does all classification and scoring.** It never
  generates free text — only typed answers (`choice`, `score`, `noul`) with probabilities.
- **The LLM (Spring AI `ChatClient`) only generates talking points, only after the gate
  passes.** It must never be used for classification, and most postings must never reach it.
- If you're about to add a code path where the LLM influences seniority/red-flag/visa
  output, or where Jev generates free text, stop — that violates this rule.

Full rationale: [docs/adr/0003-jev-llm-separation.md](docs/adr/0003-jev-llm-separation.md).

## Architecture

Hexagonal (ports and adapters), package-by-feature. Full design: [docs/architecture.md](docs/architecture.md).
Decision records: [docs/adr/](docs/adr/).

Hard boundary, enforced by ArchUnit tests in CI:
- `analysis.domain` and `analysis.application` must not import Spring, Jackson, HTTP client,
  or any provider (Jev/Spring AI) types.
- All external systems (Jev, LLM, cache) are reached only through ports owned by the
  application layer; adapters implement the ports.

## Engineering rules

- Constructor injection only. No field injection, no static state, no service locators, no Lombok.
- Domain model: immutable records, value objects, sealed types. Illegal states unrepresentable
  (e.g. `Probability` validated to `[0,1]`, `Verdict` a sealed type, not a boolean).
- Business rules (`ThresholdPolicy`, `GatePolicy`) are pure and Spring-free; unit-test them
  without a Spring context.
- No generic catch-all exception handling, no `null` returns (`Optional` or types), no
  swallowed exceptions. Errors map to RFC 9457 Problem Details in one `@RestControllerAdvice`.
- Never log posting text, candidate profile content, or secrets.
- Don't over-engineer: no speculative abstractions, no framework for its own sake. Every
  class must earn its place. Prefer deleting code to adding code.

## Verified version baseline (2026-09-21)

See [docs/adr/0001-version-baseline.md](docs/adr/0001-version-baseline.md) for sources.
Re-verify before each phase — do not trust memory for versions or APIs.

| Component | Version |
|---|---|
| Java | 25 LTS |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.x (via Boot 4.1.1 BOM) |
| Jackson | 3.0.2 (`tools.jackson.*`; annotations stay `com.fasterxml.jackson.annotation`) |
| Spring AI | 2.0.1 |
| Node.js | 24 LTS ("Krypton") |
| Vite / TypeScript / Vitest | 8.3.0 / 7.0.2 / 5.0.1 |
| Build tool | Maven |
| Extension tooling | WXT |
| License | Apache-2.0 |
| Base package | `io.github.fereshtehdev.joblens` |

## Working agreement

- Phased delivery ([docs/architecture.md](docs/architecture.md) has the phase list) — stop
  after each phase for review, don't jump ahead.
- Explain the *why* of non-obvious decisions in 1-2 sentences; write short ADRs for
  architecturally significant ones.
- The candidate profile (`profile/candidate-profile.yml`) is git-ignored; only
  `candidate-profile.example.yml` is committed.
