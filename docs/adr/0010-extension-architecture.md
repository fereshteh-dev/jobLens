# ADR-0010: Extension architecture and Phase 5 backend security

Date: 2026-09-22

## Context

Phase 5 built the extension and closed the security gap flagged in ADR-0005 (API-key auth,
CORS, request size limits were deferred until a real client existed to authenticate against).

## Decisions

### Backend

- **API-key auth and CORS default off** (`joblens.security.api-key.enabled=false`,
  `allowed-origins: []`), matching the same opt-in pattern as `joblens.ai.enabled`/
  `joblens.jev.enabled`. This keeps local dev/testing working without extra setup; real
  deployment requires explicitly turning both on. Request size limiting has no such toggle -
  it's a pure safety limit with no cost/functionality trade-off, so it's always active.
- **Constant-time key comparison via `MessageDigest.isEqual`** (JDK-native, no new
  dependency), checking every configured key rather than short-circuiting on first match, so
  the number of configured keys isn't itself a timing signal.
- **Request size limiting is `Content-Length`-based**, not a streaming byte-counter. Rejects
  before the body is read, but a chunked-encoding request with no `Content-Length` header
  passes through unchecked. Sufficient for this app's threat model (an oversized job posting
  description); a hardened streaming limit would be more complex for marginal benefit here.
- **CORS via plain `WebMvcConfigurer`**, not Spring Security. A single-origin allowlist check
  is all that's needed; pulling in `spring-boot-starter-security` for this would be the kind
  of over-engineering CLAUDE.md explicitly warns against.

### Extension

- **WXT's `browser` global, not the raw `chrome` API.** Confirmed via the actual build (not
  assumed): WXT ambient-declares `browser` (typed against the full Chrome API via
  `@wxt-dev/browser`, not just the narrower cross-browser polyfill surface), and using it
  instead of `chrome.*` is what WXT's own generated types expect - `chrome` isn't declared as
  an ambient global at all. This also simplified message-passing code, since `browser.tabs.sendMessage`
  is promise-based (no `chrome.runtime.lastError` callback dance).
- **Extraction lives in a persistent-but-idle content script**, not `chrome.scripting.executeScript`
  injected on demand. The content script (matching only `*://*.linkedin.com/*`) does nothing
  on load or navigation - it only extracts and responds when it receives an `EXTRACT_POSTING`
  message, which only happens when the user clicks "Analyze this posting" in the side panel.
  Satisfies "read only on user action, no crawling" either way; this approach was simpler to
  get right with WXT's conventions than dynamic script injection.
- **The side panel calls the backend directly** (not routed through the background service
  worker). Side panels are full extension page contexts and can `fetch()` directly; adding a
  background-relay hop would be pure ceremony for no benefit here.
- **`browser.storage.local`, not `.sync`**, for the API key - a secret-like value shouldn't
  sync across the user's Google account/devices.
- **`render.ts` is pure formatting**, deliberately separated from `main.ts`'s DOM/chrome-API
  wiring so it can be unit-tested without a real extension context. It only ever displays
  what the backend's response already decided (verdict kind/probability, gate pass/fail and
  reasons) - no thresholds, no re-classification, keeping "no business logic in the
  extension" true in the code, not just in intent.

## Known gaps, flagged rather than silently left

- **LinkedIn's selectors in `LinkedInAdapter.ts` are best-effort, not verified against a live
  page.** This environment has no way to browse to an authenticated LinkedIn job posting and
  inspect real DOM/class names; the selectors are based on commonly-documented LinkedIn class
  naming patterns. The `SiteAdapter` interface exists specifically so fixing this later is a
  one-file change - but expect it to need fixing.
- **No custom icons.** The manifest builds and Chrome will show a default icon; polishing this
  is deferred to Phase 6 (repo polish) rather than blocking Phase 5's actual goal (wiring to
  the backend).
- **Not tested in an actual loaded Chrome instance.** Verified: `tsc --noEmit` is clean,
  `vitest run` passes (13 tests: render formatting, LinkedIn adapter extraction against
  synthetic HTML via `jsdom`'s `JSDOM`), and `wxt build` produces a manifest with the expected
  permissions/content-script/side-panel/options wiring. Not verified: actually loading the
  unpacked extension in `chrome://extensions` and clicking through the real flow. Do this
  manually - see the phase summary for exact steps.
- **Local Node is v22.12.0, not the Node 24 LTS baseline from ADR-0001.** Didn't block
  anything (`npm install`/`tsc`/`vitest`/`wxt build` all succeeded, just with `EBADENGINE`
  warnings from `jsdom` and a couple of its transitive deps wanting `>=22.19`/`^22.22`+).
  Worth upgrading Node before relying on this long-term.
