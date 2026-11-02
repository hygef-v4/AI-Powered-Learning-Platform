# Security and contract checks — U14

From `frontend/`, run `npm audit --omit=dev --json` and retain the result separately from full development dependency findings. Latest production audit:0vulnerabilities; this does not cover all development dependencies. Do not apply unrelated automatic major upgrades.

Validate `docs/contracts/openapi/notifications.yaml` as OpenAPI3.1 with local references, then compare its12operations/9paths to real responses, error codes, cookie authentication and CSRF enforcement. Syntax/reference smoke already passed; independent runtime contract acceptance is pending.

With real sessions, test four roles, assigned/nonassigned classes, inactive accounts and direct foreign IDs. Verify self-only notifications/preferences/SSE, staff preference denial, Admin class denial, CSRF-required mutations, expired/revoked sessions and409stale versions. Use adversarial Markdown/URLs and verify no executable raw HTML, script URL or image leakage.

Inspect production headers and HTML through actual HTTPS/Nginx: fresh nonce per request matching every script, overwritten caller nonce/CSP, no public caching of personalized nonce HTML, correct frame restrictions, HSTS/nosniff/referrer. Confirm permitted PDF/YouTube/Drawio embedding still works. Check logs/audit omit raw announcement body, email, tokens and provider exception payloads.

Developer HTTP/browser fixture checks passed; real proxy/session/provider security and fault behavior still require independent evidence. Enabled Security/Resiliency rules and unresolved gates remain recorded in [code summary](../u14-announcements-and-notifications/code/code-summary.md); PBT remains disabled.
