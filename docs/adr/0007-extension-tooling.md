# ADR-0007: WXT for the extension build

Date: 2026-09-21

## Context

Brief asks for a Vite-based MV3 build and to pick between WXT and CRXJS, with justification.

## Decision

WXT.

## Why

WXT is actively maintained, targets MV2/MV3 across Chromium/Firefox/Safari from one codebase,
and includes first-class support for the pieces this extension needs (content scripts, side
panel, options page) with fast HMR. It's pre-1.0 (`~0.21.x`) but described by its own docs as
production-ready and is the more actively developed of the two as of 2026-09-21; CRXJS has
seen substantially less recent activity. Given the extension is intentionally thin (no
business logic), WXT's opinionated structure is a net win, not a constraint.

## Consequences

Re-check WXT's release notes at the start of Phase 5 in case a 1.0 release changed any APIs
used here.
