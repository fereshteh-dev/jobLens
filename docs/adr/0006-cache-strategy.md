# ADR-0006: Analysis cache

Date: 2026-09-21

## Context

Repeated analysis of the same posting (user re-opens a tab, re-runs the extension) shouldn't
re-hit Jev/the LLM — both for cost and latency.

## Decision

Caffeine (`3.3.0`), in-process, keyed by SHA-256 of the normalized posting description text
(trim, collapse whitespace, lowercase before hashing — normalization lives in
`analysis.domain` as a pure function so the fingerprint is deterministic and testable without
the cache adapter). Bounded size + time-based expiry, both configurable.

## Why

Single backend instance, no cross-instance sharing requirement in the brief; Caffeine is the
simplest thing that works and is already implied by the suggested package structure
(`adapter/out/cache/`). Revisit only if the project grows to multiple backend instances,
which would need a shared store (e.g. Redis) instead.
