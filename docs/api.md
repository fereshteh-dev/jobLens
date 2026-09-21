# API

The full, current contract is generated automatically (springdoc-openapi, verified against
Boot 4.1.1 in Phase 6 — see [ADR-0001](adr/0001-version-baseline.md)) and served by the
running backend itself — this file is a short human map, not a duplicate spec that can drift.

With the backend running locally:

- **Interactive docs:** http://localhost:8080/swagger-ui/index.html
- **Raw OpenAPI JSON:** http://localhost:8080/v3/api-docs

## `POST /api/v1/analyze`

```
request:  { url, title, company, location, descriptionText }
response: { seniority, redFlags[], sponsorship, gate: { passed, reasons[] }, talkingPoints? }
```

- `talkingPoints` is present **only** when `gate.passed` is `true` **and** the LLM adapter
  succeeded (see [ADR-0003](adr/0003-jev-llm-separation.md) and
  [ADR-0005](adr/0005-resilience-and-rate-limiting.md)). Its absence with `gate.passed: true`
  means the LLM call failed after retries — not that the request itself failed.
- Every probability-derived field carries a `Verdict` (`CONFIDENT` / `UNCERTAIN` / `IGNORED`)
  — see `AnalyzeResponse.java` and `docs/architecture.md` §6.

### Error responses (RFC 9457 Problem Details)

| Status | When |
|---|---|
| `400` | Request validation failed, or a domain-level `IllegalArgumentException`. |
| `401` | `joblens.security.api-key.enabled=true` and the `X-API-Key` header is missing/wrong. |
| `413` | Request body exceeds `joblens.security.max-request-body-size`. |
| `429` | Rate limit exceeded (`joblens.rate-limit.*`). |
| `502` | Jev classification failed after retries/circuit breaker — see ADR-0009. |
| `500` | Anything else unexpected. Never includes a stack trace. |

### Auth

Send `X-API-Key: <your key>` if `joblens.security.api-key.enabled=true` (off by default — see
ADR-0010). The extension's options page is where you configure the key it sends.
