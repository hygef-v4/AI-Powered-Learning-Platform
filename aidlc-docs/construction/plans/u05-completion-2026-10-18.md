# U05 completion against existing contracts

## Authorization and decisions
User request: "giúp tôi haonf thiện u05" following the reviewed missing service/API/wiring findings and the team's agreement to code units independently and merge later. This supersedes the former independent-domain-only restriction. Continue existing approved functional design (UC20-30), not a new inception. Retain current Security/Resiliency subset and disabled PBT. No implementation of another unit, no fake authentication, no production DB mutation, deployment, commit or push.

Ruling: Complete U05 against the existing interfaces using injected contracts and mocks in unit tests; unresolved live cross-unit behavior remains an explicit integration check, not a reason to leave services unwritten.
Ruling: U05 list APIs return JSON arrays with page/size (1-based, maximum 100) and optional q/status filters; lookup candidates return arrays capped at 20. Details match existing frontend types. UI adapters explicitly handle API errors and optimistic versions.
Ruling: Keep SQL with real FKs staged until accounts/subjects migrations exist; keep persistence-dependent U05 beans gated together by platform.academics.persistence-enabled. Do not invent upstream tables or weaken authorization stubs.

## Plan
- [x] Read current contracts and U05 design; establish clean branch baseline and API conventions.
- [x] Implement/query-test scoped JDBC reads, counts and real U05 contract adapters.
- [x] Implement/test application lifecycle, enrollment, scopes, audit/events and REST APIs using real contracts.
- [x] Implement/test frontend API adapters, real routes, loading/errors and slot hosts while keeping preview isolated.
- [x] Review combined behavior and security boundaries; fix integration defects.
- [x] Run backend/frontend checks, update design/code status and handoff checklist with honest integration limits.

## Ownership and verification
Use bounded subagent implementation per subagent-driven-development skill: backend application/web/tests, frontend features/routes/tests, coordinator repository/contracts/migration/docs. No overlapping writers. Maximum three agents including coordinator until review, depth one, max three repair rounds; no recursive delegation or commits. Run TDD for new behavior, integrated Maven tests/architecture, lint/typecheck/Vitest/build and focused API contract/error checks. Independent final review must cover spec and security. Source writes outside the chat's original workspace require tool escalation.

## Final evidence
- Clean full Maven test run: 164 tests across 33 suites, zero failures/errors/skips; includes three architecture checks and 70 U05 backend tests. Nested DTO input interface renamed IStrictInput to satisfy the established I-prefix rule; stale class output removed by Maven clean.
- Final combined frontend run from coordinator: lint/typecheck exit 0; 70 tests across 18 files pass. Final production Next.js build passes all U05 production routes and guarded preview; BUILD_ID produced after the last source repair.
- Query/contract tests cover scoped bindings, literal wildcard search, current teacher/enrollment revocation, multi-page contract lists and replacing upstream stub beans only when explicitly enabled.
- Application/API tests cover current actor/role/scope, distinct write authorization, version/reopen conflicts, row outcomes and eligibility, separate batch transactions, audit timing, activation events, validation/principal handling and immutable-field rejection.
- Independent review found one P2: PATCH409 reload remounted the version-keyed InfoEditor and lost its visible error. Exact new regression failed before repair; a route-level conflict notice now survives reload, and a retry sends the server's refreshed version2. Scoped independent re-review confirmed the finding resolved; no other actionable findings remained.
- Docker CLI cannot reach a running daemon; live PostgreSQL migration/lock/concurrency and real upstream authenticated browser flows were not run. These are integration gates, not missing U05 service/controller code. No commit/push/deployment or real business data writes.
- Handoff and activation checklist: `../u05-class-enrollment/code/completion-2026-10-18.md`.
