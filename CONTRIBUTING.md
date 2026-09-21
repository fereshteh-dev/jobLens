# Contributing to JobLens

Thanks for considering a contribution. This is a small, personal-scale open-source project -
please read [CLAUDE.md](CLAUDE.md) first. It states the project's one non-negotiable rule
(Jev classifies, the LLM only writes talking points after the gate passes) and the engineering
rules the codebase follows; a PR that violates either will need rework before it can merge.

## Before you start

For anything beyond a small fix (a new adapter, a new endpoint, a dependency change), please
open an issue first to discuss the approach. It's much easier to agree on a design before code
exists than to rework a finished PR.

## Project layout

- `backend/` - Spring Boot API. See [docs/architecture.md](docs/architecture.md) for the
  hexagonal layout (`domain` / `application` / `adapter`) and [docs/adr/](docs/adr/) for why
  things are the way they are.
- `extension/` - the Chrome MV3 client. Thin by design - no business logic belongs here (see
  ADR-0010).
- `docs/` - architecture, ADRs, API notes.

## Backend workflow

```
cd backend
./mvnw test              # full test suite, including ArchUnit boundary checks
./mvnw spring-boot:run    # run locally against mock adapters (no API keys needed)
```

- Domain and application code (`analysis/domain`, `analysis/application`) must stay free of
  Spring, Jackson, HTTP-client and adapter-package imports - `HexagonalArchitectureTest`
  enforces this and will fail the build if you cross the boundary.
- New adapters get their own unit/adapter test. For an HTTP-calling adapter, prefer Spring's
  built-in `MockRestServiceServer` over adding a new mocking dependency (see ADR-0009).
- Keep pure domain policies (`ThresholdPolicy`, `GatePolicy`) Spring-free and unit-tested
  without a Spring context.

## Extension workflow

```
cd extension
npm install
npm run compile   # tsc --noEmit
npm test           # vitest run
npm run build       # wxt build -> extension/.output/chrome-mv3
```

Load `extension/.output/chrome-mv3` as an unpacked extension via `chrome://extensions` to test
manually against a running backend.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`,
`refactor:`, `test:`, `chore:`). Keep commits small and focused - one logical change each.

## Pull requests

- Make sure `./mvnw test` (backend) and `npm run compile && npm test` (extension) pass before
  opening a PR - CI runs the same checks and will block merge otherwise.
- Describe *why*, not just *what*, especially for anything touching the gate, the resilience
  policies, or what gets logged (never posting text, candidate profile content, or secrets -
  see CLAUDE.md).
- Small, focused PRs are much easier to review than large ones bundling unrelated changes.

## Reporting bugs / requesting features

Open a GitHub issue. For security vulnerabilities, see [SECURITY.md](SECURITY.md) instead -
please don't open a public issue for those.
