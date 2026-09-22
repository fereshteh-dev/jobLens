# ADR-0011: Findings from live testing with real provider keys

Date: 2026-09-22

## Context

Every prior phase's "real Jev/LLM adapter" work was verified against mocks, unit tests, or a
deliberately-unreachable endpoint (to test failure handling) - never a live call with real
credentials, because no real API key was available in this environment until now. This session
had real Jev and LLM keys for the first time. Two real bugs and one architecture change came
out of it.

## Bug 1: `StubTalkingPointsWriter` was never gated

`SpringAiTalkingPointsWriter` got `@ConditionalOnProperty(..., havingValue = "true")` in
Phase 3. `MockPostingClassifier` got the matching `havingValue = "false", matchIfMissing = true`
condition in Phase 4, once a second `PostingClassifier` existed. The equivalent condition was
never added to `StubTalkingPointsWriter` back in Phase 3, when it first became one of two
`TalkingPointsWriter` beans. Nothing caught this because `joblens.ai.enabled=true` was never
actually exercised end-to-end before now - every earlier smoke test either left it `false` or
only tested `JevPostingClassifier` in isolation (Phase 4's deliberately-unreachable-host test).
The moment `joblens.ai.enabled=true` was set for a real run, Spring Boot failed to start:
`required a single bean, but 2 were found: springAiTalkingPointsWriter, stubTalkingPointsWriter`.

**Fixed**: added `@ConditionalOnProperty(prefix = "joblens.ai", name = "enabled", havingValue = "false", matchIfMissing = true)`
to `StubTalkingPointsWriter`, mirroring `MockPostingClassifier`.

## Bug 2: `minimumSponsorshipLikelihood = UNKNOWN` didn't mean "no requirement"

`candidate-profile.example.yml` documents: *"Use UNKNOWN here if you don't need visa
sponsorship or relocation support at all."* `GatePolicy` used `SponsorshipLikelihood.meetsMinimum`,
a plain ranked comparison, for this - and `UNKNOWN`'s rank (2) sits above `UNLIKELY` (1) and
`EXPLICIT_NO` (0). So a minimum of `UNKNOWN` silently rejected any posting whose actual
sponsorship/relocation likelihood came back `UNLIKELY` or `EXPLICIT_NO` - the opposite of "I
don't care." A real posting analyzed live (real seniority match, real red flags, real Jev
data) failed the gate for exactly this reason: relocation came back `UNLIKELY`, minimum was
the documented default `UNKNOWN`, and `UNLIKELY.meetsMinimum(UNKNOWN)` was `false`.

**Fixed**: `GatePolicy` now skips both sponsorship checks entirely when
`profile.minimumSponsorshipLikelihood() == SponsorshipLikelihood.UNKNOWN`, matching the
documented behavior. The rank ordering itself (`EXPLICIT_NO < UNLIKELY < UNKNOWN < LIKELY <
EXPLICIT_YES`) is left unchanged - it's still correct for a candidate with a real requirement
(e.g. minimum `LIKELY` should still reject `UNKNOWN`, and that's unaffected). Regression test:
`GatePolicyTest.minimumSponsorshipUnknown_meansNoRequirement_evenBelowExplicitNo`.

Both bugs share a root cause worth naming: **the conditional-bean pattern and the
"UNKNOWN-as-wildcard" gate semantics were each implemented once, then never re-exercised
end-to-end when a second case that depended on them showed up.** Neither was caught by the
existing test suite because the tests exercised the two halves (mock-mode wiring; gate logic
with concrete non-`UNKNOWN` values) but never the specific combination a live run hit
immediately. Worth remembering when adding a third classifier/writer or a fourth sponsorship
tier.

## Decision: LLM provider switched from Anthropic to OpenAI

The user's available LLM credentials are for OpenAI (a `sk-proj-...` key), not Anthropic.
Requested and confirmed as a permanent switch, not a one-off test.

- `spring-ai-starter-model-anthropic` → `spring-ai-starter-model-openai` (both `2.0.1`).
- `spring.ai.anthropic.*` → `spring.ai.openai.*`; `ANTHROPIC_API_KEY` → `OPENAI_API_KEY`
  throughout (`application.yml`, `.env.example`, `docker-compose.yml`).
- Model: `gpt-5.6-terra` (OpenAI's current "balances intelligence and cost" tier - verified
  against `developers.openai.com/api/docs/models` at switch time, not assumed).
- `SpringAiTalkingPointsWriter` and its test needed **zero code changes** - Spring AI's
  `ChatClient` abstraction is genuinely provider-neutral at that layer, which is exactly the
  point of using it. Only the dependency and config changed.
- README's non-affiliation disclaimer, privacy section, and architecture diagram; the
  extension's privacy-notice text; and `SECURITY.md`'s scope section all updated to say
  OpenAI instead of Anthropic.

## What was and wasn't verified live in this session

- ✅ Jev: real classification, real probabilities, confirmed end-to-end with live data. One
  spurious connect-timeout on the very first call after a fresh start, in the same run that
  first pulled in `spring-ai-starter-model-openai` - Netty's one-time native-library
  load/warm-up (visible in the logs) plausibly ate into the 5s connect-timeout; a retry
  moments later succeeded normally and every other call that run was fine. Not chased further
  since it didn't recur, but worth knowing if a *first* request after startup times out and
  later ones don't.
- ✅ Both bugs above: found and fixed via live testing, not caught by the prior test suite.
- ✅ The OpenAI key is valid and authenticates correctly - confirmed by a real, specific error
  from OpenAI itself (`429: You have no credits remaining`), not a generic auth failure. The
  request reached OpenAI, was processed, and got a real business response back.
- ✅ Graceful LLM-failure degradation: proven against four different *real* failure modes in
  sequence (wrong provider/base-URL, a proxy's client-rejection error, an exhausted-credits
  429, and Jev's own transient timeout above) - the request never crashed, `talkingPoints` was
  correctly omitted each time, per ADR-0003/0005.
- ❌ Not yet verified: a *successful* `talkingPoints` generation end-to-end. Blocked purely on
  OpenAI account billing (add credits at platform.openai.com), not on anything in this
  codebase - the failure this surfaced is external to the project.
- An attempt to route through `agentrouter.org` (a third-party free relay claiming
  Anthropic-and-OpenAI-compatible endpoints) failed consistently across 3 different keys, 2
  endpoint styles, 2 auth header styles, and multiple `User-Agent` values, always with the
  same `unauthorized_client_error` - conclusively a server-side gate on their end unrelated to
  this project's code, not pursued further.

## Update: default LLM route switched to AgentRouter

`spring.ai.openai.base-url` and `spring.ai.openai.chat.options.model` now default to
AgentRouter's OpenAI-compatible endpoint (`https://co.agentrouter.org/v1`, model
`gpt-6-astra`) rather than `api.openai.com` directly - both overridable via `OPENAI_BASE_URL`
/ `OPENAI_MODEL` (see `backend/src/main/resources/application.yml`). The API key env var was
also renamed `OPENAI_API_KEY` → `AGENTROUTER_KEY`, and must now be an AgentRouter key by
default, not a `platform.openai.com` key, unless `OPENAI_BASE_URL` is overridden back to
`https://api.openai.com/v1`.

Spring AI 2.0.1's `spring-ai-openai` module wraps the official OpenAI Java SDK
(`com.openai:openai-java-core`), whose default base URL already includes `/v1`
(`com.openai.core.ClientOptions`) - unlike the pre-2.0 community client, the SDK does not
append `/v1` itself, so any override must include it (e.g. `.../v1`, not the bare host).

This doesn't erase the finding above: the earlier attempt to route through `agentrouter.org`
failed with `unauthorized_client_error` across every combination tried at the time. This
switch uses a different subdomain (`co.agentrouter.org` vs. whatever was tried before) and
model alias, and has **not yet been verified live** - the first real request against
`joblens.ai.enabled=true` will confirm whether it authenticates.

## Update: switched to AgentRouter's native Messages-format endpoint

The `co.agentrouter.org` OpenAI-compat route above got past the earlier `unauthorized_client_error`
but then failed with `401: Invalid API Key!`, then `CircuitBreaker 'talkingPoints' is OPEN`
once the breaker tripped from the first failure - root cause turned out to be unrelated to
this endpoint choice: the IntelliJ run config's env var was still named `OPENAI_API_KEY`
(left over from the pre-AgentRouter OpenAI setup) instead of `AGENTROUTER_KEY`, so
`spring.ai.*.api-key` was silently resolving to the `not-configured` placeholder. Fixed by
renaming the run config's env var - config precedence, not provider/endpoint, was the bug.

Separately, requested and confirmed: route through AgentRouter's **native** endpoint
(`https://agentrouter.org/v1/messages`, Anthropic Messages format) instead of the
OpenAI-compat one, on grounds that it's more likely to be AgentRouter's actual first-class
API rather than a translation shim.

- `spring-ai-starter-model-openai` → `spring-ai-starter-model-anthropic` (both `2.0.1`).
- `spring.ai.openai.*` → `spring.ai.anthropic.*`; `OPENAI_BASE_URL` / `OPENAI_MODEL` →
  `AGENTROUTER_BASE_URL` / `AGENTROUTER_MODEL`. `AGENTROUTER_KEY` unchanged.
- `base-url` defaults to the bare host `https://agentrouter.org`, **no** `/v1` suffix -
  unlike `spring-ai-openai`'s wrapped SDK (which needed the full `/v1` in the override, per
  the note above), the Anthropic Java SDK that `spring-ai-anthropic` 2.0.1 wraps appends
  `/v1/messages` to the base URL itself. Given the two modules' opposite conventions here,
  re-verify this against the SDK actually in use if either module is upgraded.
- `SpringAiTalkingPointsWriter` needed zero code changes again, for the same reason as the
  original Anthropic→OpenAI switch: it only depends on `ChatClient`/`ChatClient.Builder`.
- Model alias left as `gpt-6-astra` (the same one that worked, config-wise, on the
  OpenAI-compat route) since AgentRouter's `model` field is what selects the backend
  regardless of wire format - **not verified** that this specific alias is valid on the
  Messages-format endpoint specifically; if the next live run 404s or rejects the model,
  that's the first thing to check.
- README, `.env.example`, `docker-compose.yml` updated for the renamed env vars and the
  endpoint description; the "OpenAI-compatible" / "GPT" wording in README was replaced with
  wording that doesn't assume a specific model family (AgentRouter's routing target isn't
  something this project controls or verifies).
- This does **not** reopen the Jev/LLM separation question (ADR-0003) or reintroduce
  Anthropic as a *classification* provider - Jev remains the only classifier. This is purely
  which wire format/module reaches AgentRouter for talking-points generation.

**Not yet verified live** - same caveat as every routing change in this ADR: only a real
request against `joblens.ai.enabled=true` confirms whether this specific
endpoint+key+model combination actually authenticates and returns a usable response.

## Update: dropped AgentRouter, calling Anthropic's API directly

Requested and confirmed: stop relaying through AgentRouter altogether and call Anthropic's
own API (`api.anthropic.com`) directly for talking-points generation.

- `spring-ai-starter-model-anthropic` stays (it was already the right module for Anthropic's
  Messages format - the previous update just had it pointed at AgentRouter as a relay).
- `spring.ai.anthropic.base-url` override removed entirely - `api.anthropic.com` is
  `spring-ai-anthropic`'s built-in default.
- `AGENTROUTER_KEY` / `AGENTROUTER_BASE_URL` / `AGENTROUTER_MODEL` → `ANTHROPIC_API_KEY` /
  (no base-url var) / `ANTHROPIC_MODEL`. The key must now be issued by
  console.anthropic.com - an AgentRouter or OpenAI key will not work here.
- Model changed from the AgentRouter alias `gpt-6-astra` to `claude-sonnet-5`, an actual
  Anthropic model id.
- `SpringAiTalkingPointsWriter` again needed zero code changes.
- README, `.env.example`, `docker-compose.yml`, `SECURITY.md` updated for the renamed env
  var and to describe Anthropic instead of AgentRouter as the talking-points provider.
- **Known gap**: the `.idea/workspace.xml` run config's `ANTHROPIC_API_KEY` value is the
  same key string carried over from the original `OPENAI_API_KEY` → `AGENTROUTER_KEY`
  rename two updates ago (`sk-...`, not the `sk-ant-...` shape Anthropic keys use) - it has
  **not** been replaced with a real console.anthropic.com key, so the next live run is
  expected to fail auth until that's done.
- This still doesn't touch ADR-0003: Jev remains the sole classifier; Anthropic is reached
  only for talking points, only after the gate passes.

## Update: switched to OpenRouter (requested and confirmed as permanent)

Root cause of the persistent 401s on the "Anthropic direct" route above, found via live
testing this session: **two different bad keys**, not the provider or the wiring.

- The shell-level `ANTHROPIC_API_KEY` env var was correctly *shaped* (`sk-ant-api03-...`,
  right length, no stray whitespace/quotes) but Anthropic's API itself rejected it directly
  (`{"type":"authentication_error","message":"API key is invalid."}`) - confirmed with a raw
  HTTP call to `api.anthropic.com`, bypassing this project's code entirely, so this was never
  a config-wiring bug.
- Separately, `.idea/workspace.xml`'s `ANTHROPIC_API_KEY` run-config entry still held the old
  AgentRouter-shaped key value (not even Anthropic's key shape), carried over unedited from
  the AgentRouter/OpenAI phase further up this ADR. Exactly the gap the "dropped AgentRouter"
  update above already flagged and never fixed. This file is
  git-ignored/local, so it wasn't touched by this change - fix it by hand in IntelliJ's Run
  Configurations.

Rather than chase a fourth Anthropic key, requested and confirmed: switch the LLM route to
OpenRouter (`openrouter.ai`) permanently, using its free tier.

- `spring-ai-starter-model-anthropic` → `spring-ai-starter-model-openai` (both `2.0.1`) -
  OpenRouter exposes an OpenAI-compatible endpoint.
- `spring.ai.anthropic.*` → `spring.ai.openai.*`; `ANTHROPIC_API_KEY` / `ANTHROPIC_MODEL` →
  `OPENROUTER_API_KEY` / `OPENROUTER_MODEL`, plus a new `OPENROUTER_BASE_URL` (default
  `https://openrouter.ai/api/v1` - the OpenAI Java SDK `spring-ai-openai` wraps needs the
  full `/v1` path in any override, same as the OpenAI-direct phase earlier in this ADR).
- Model: `z-ai/glm-5.2:free` - a real free-tier model id, verified live against
  `openrouter.ai/api/v1/models` at switch time (the initially-pasted `openrouter/free` isn't
  a real model and would have 404'd on the first request). Free-tier ids rotate on
  OpenRouter; re-check that endpoint if this one stops resolving.
- `SpringAiTalkingPointsWriter` needed zero code changes again, same reason as every prior
  switch in this ADR: it only depends on `ChatClient`/`ChatClient.Builder`.
- README (non-affiliation, architecture diagram, privacy section, env var table, known
  limits), `.env.example`, `docker-compose.yml`, `SECURITY.md`'s out-of-scope note, and the
  extension's privacy-notice text all updated to say OpenRouter instead of Anthropic.
- This still doesn't touch ADR-0003: Jev remains the sole classifier; OpenRouter is reached
  only for talking points, only after the gate passes.

**Not yet verified live** - same standing caveat as every routing change in this ADR: only a
real request against `joblens.ai.enabled=true` with a real `OPENROUTER_API_KEY` confirms this
specific endpoint+key+model combination actually authenticates and returns a usable response.

## Update: auth confirmed working; default model swapped for free-pool overload

Auth verified live end-to-end. Root cause of the 401s wasn't the key or the app - it was the
`.idea/workspace.xml` run config's `<envs>` block, which still explicitly passed the old
`ANTHROPIC_API_KEY`/stale-value pair and had no `OPENROUTER_API_KEY` entry at all. IntelliJ run
configs' explicit `<envs>` don't inherit new OS-level env vars automatically - each provider
switch in this ADR kept missing that file. Fixed by editing it directly (git-ignored/local, so
this doesn't touch committed history).

Once auth was fixed, the first live request failed differently: `RateLimitException: 429:
Provider returned error`. Direct testing against `openrouter.ai/api/v1/chat/completions`
(bypassing the app) showed this was OpenRouter's free tier itself, not a config problem:

```
"z-ai/glm-5.2:free is temporarily rate-limited upstream... provider_error_code":"overloaded",
"limit_source":"upstream_provider_shared_pool", "retry_after_seconds":5
```

Free OpenRouter models share a capacity pool across all free users on that model/provider
pairing - it can stay saturated for extended periods, not just a brief blip. 12 retries with
the suggested 5s backoff, spanning about a minute, all still 429'd on `z-ai/glm-5.2:free`. The
existing `joblens.resilience.talking-points` config (`retry-max-attempts: 3`,
`retry-wait-duration: 500ms`, fixed not exponential) can't ride out a 5s-scale outage like
this - it's tuned for transient network blips, not shared-pool saturation, and wasn't changed
here since that's a separate, deliberate resilience-policy decision, not a routing fix.

Probed four other free models in parallel at the same moment: `qwen/qwen3.8-27b:free`,
`google/gemma-4-31b-it:free`, and `google/gemma-4-26b-a4b-it:free` were all also 429ing, but
`liquid/lfm-2.5-2.6b:free` responded immediately (200). Switched the default
(`OPENROUTER_MODEL` / `application.yml`) to `liquid/lfm-2.5-2.6b:free` on that basis - it's
whichever free model has headroom *right now*, not a permanent guarantee. Any free-tier model
can hit the same shared-pool overload later; re-check `openrouter.ai/api/v1/models` and swap
`OPENROUTER_MODEL` if this one starts 429ing persistently too. A paid OpenRouter balance (even
a small one) routes off the shared free pool entirely and would remove this class of failure,
but that's a cost/reliability tradeoff for whoever runs this deployment to decide, not something
to default to silently.

**Verified live**: talking-points generation now returns a real response end-to-end via direct
API testing with the configured key and model. Not yet re-confirmed through a live request in
the running app itself with this exact model swap - the next `joblens.ai.enabled=true` run
through the app is the final check.
