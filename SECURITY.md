# Security Policy

JobLens is a personal-scale, open-source project (see [README.md](README.md) and
[CLAUDE.md](CLAUDE.md) for what it is and isn't). This policy is intentionally small.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for a security vulnerability. Instead, use
GitHub's private vulnerability reporting for this repository (Security tab → "Report a
vulnerability"), or open a draft security advisory. Include:

- What you found and where (file/endpoint).
- Steps to reproduce, if applicable.
- Impact you'd expect (what an attacker could do with it).

You should get an initial response within a few days. There's no bug bounty - this is an
unfunded personal project - but reports are taken seriously and credited unless you ask
otherwise.

## Scope

In scope:
- The backend (`backend/`): the Spring Boot API, its auth/rate-limiting/CORS handling, and
  how it calls Jev and the LLM provider.
- The extension (`extension/`): what it reads from a page, what it stores, and what it sends.

Out of scope:
- Vulnerabilities in Jev/TypeSafe AI, OpenRouter (or any model it routes to), or LinkedIn's
  own services - report those to them directly.
- Missing hardening for a scenario the README's "Known limits" section already documents as a
  known, accepted gap (e.g. the LinkedIn selectors breaking when LinkedIn changes its markup).

## What this project does and doesn't protect against

See [docs/adr/0005-resilience-and-rate-limiting.md](docs/adr/0005-resilience-and-rate-limiting.md)
and [docs/adr/0010-extension-architecture.md](docs/adr/0010-extension-architecture.md) for the
current, honest state of things - including gaps that are flagged rather than hidden (for
example: request-size limiting is `Content-Length`-based, not a streaming byte-counter).

## Secrets

Provider API keys (`ANTHROPIC_API_KEY`, `JEV_API_KEY`) and the candidate profile
(`backend/profile/candidate-profile.yml`) must never be committed - see `.gitignore` and
`.env.example`. If you accidentally commit a real secret, rotate it immediately; removing it
from a later commit does not remove it from git history.
