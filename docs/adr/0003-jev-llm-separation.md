# ADR-0003: Jev for classification, LLM only for gated talking points

Date: 2026-09-21

## Context

This is the project's one non-negotiable rule, restated here so the *why* is on record, not
just the *what* (already in [CLAUDE.md](../../CLAUDE.md)).

## Decision

`PostingClassifier` (implemented by `JevPostingClassifier`) is the only source of seniority,
red-flag, and visa/relocation judgments — always called, for every posting. `TalkingPointsWriter`
(implemented by `SpringAiTalkingPointsWriter`) is only called by `AnalyzePostingUseCase` after
`GatePolicy.evaluate()` returns `passed = true`.

## Why

- Jev returns typed, probability-scored answers to fixed questions — exactly the shape needed
  for structured, auditable judgments ("real vs. claimed seniority", a fixed red-flag
  taxonomy). An LLM asked the same questions in free text would need a second parsing/
  validation layer and would silently drift in output shape across calls.
- Cost and latency: most postings should never reach the LLM. Gating on Jev's classification
  keeps the expensive path rare by construction, not by convention — the use case's control
  flow makes it structurally impossible to reach the writer before the gate passes.
- Failure isolation: if the LLM provider is down, the classification (the core product) still
  works; only talking points are missing. If Jev is down, there's nothing worth showing, so
  the use case fails fast rather than falling back to the LLM for classification.

## Consequences

- `AnalyzePostingUseCase` must not have any code path that calls `TalkingPointsWriter` before
  `GatePolicy` has produced a `passed = true` `GateDecision`. This is worth a dedicated unit
  test (verify the writer port is never invoked when the gate fails), not just an assumption.
- No requirement or future ticket should ask Jev to produce free text, or ask the LLM to
  produce a classification/score. Treat such a request as a spec smell and push back.
