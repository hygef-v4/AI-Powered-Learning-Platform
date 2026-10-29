# MCP and AI readiness audit

Scope approved by the user's request to inspect configuration, correct evidenced issues, and provide self-test instructions. Work directly in the current checkout; preserve unrelated untracked files and existing credentials. Do not create provider connections, change models, publish grades, or edit the unrelated Unity MCP registration.

- [x] Inspect repository, current runtime, agent configuration metadata, MCP transport, and connection/task metadata without exposing secrets.
- [x] Add a read-only MCP diagnostic with secure token input; verify success, invalid/error responses, and HTTP authorization failure using a local fixture.
- [x] Correct stale connection guidance and document separate agent/provider credentials and source availability.
- [x] Run focused MCP and AI workflow regression tests; restore API/worker runtime and check health.
- [x] Record findings, limitations, and a manual acceptance guide. Authenticated client acceptance requires a platform token created by the user; do not label an unauthenticated HTTP response as a successful agent connection.
- [x] User approved a 7-day student READ_ONLY credential; Codex CLI global registration discovered platform tools and completed one authorized material read.
- [x] Fresh teacher Gemini connection check and disposable assignment AI draft generation/application/persistence verified in the browser; report and manual guide updated.
- [x] Teacher-scoped token connected to the existing Codex registration; MCP protocol/tool discovery and one read-only QA assignment context call succeeded.
- [ ] Remaining user acceptance: AI quiz draft, grading proposal on a test submission, Practice feedback, and fractional manual grading boundaries. See the report and manual guide in construction/build-and-test.

Acceptance: no writes to grades or provider keys; diagnostic never calls tools/call; token absent from output; precise distinction between tested backend behavior and pending real agent/AI UI tests.
