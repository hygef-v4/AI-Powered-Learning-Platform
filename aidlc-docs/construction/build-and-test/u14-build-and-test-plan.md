# U14 Build & Test execution plan — 2026-10-21

User approved code and continuation via “tiếp tục”. This is scoped U14 readiness work; the workflow's global all-units-complete prerequisite is not satisfied.

- [x] Review actual Maven/npm/CI/local compose and existing tester matrix.
- [x] Mark code-generation step11 approved; preserve independent acceptance gates.
- [x] Run full final-source backend verify:921total/844pass/77skip/0failure/error and executable JAR.
- [x] Retain final frontend314pass/lint/typecheck/build/security smoke evidence; no source changes requiring rerun.
- [x] Check Docker: CLI present, Engine unavailable. Identify missing real U07/U08 migrations and stub U07/U08/U12 providers.
- [x] Prepare build, unit, integration, performance, security and E2E instructions plus scoped summary.
- [x] Validate documentation links/whitespace and record audit/state; no commit/push.
- [ ] Independent real PostgreSQL/provider/Rabbit/Mailpit/session/SSE integration and security acceptance after prerequisites.
- [ ] Independent browser/mobile E2E and measured performance/fault/scanner-cardinality evidence.
- [ ] Review remaining acceptance evidence before global stage completion or Operations. Current approval is not deployment authorization.
