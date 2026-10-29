# U05 unit test expansion

## Scope and authorization
User request: "giúp tôi bổ sung unit test cho u05" after review identified untested role/scope/read paths, later-row batch failures and API validation/mapping. Resume current U05 workflow; preserve all existing uncommitted implementation and retain extension settings. No deployment, database writes, commit or push.

## Plan
- [x] Inspect current tests and implementation; distinguish U05 counts from whole-project totals.
- [x] Add service role/action/scope matrix and successful read/summary/candidate tests with meaningful negative assertions.
- [x] Add API request/response, validation and server-principal mapping cases across current endpoints.
- [x] Add observable batch transaction tests for successful prior lines, later failure, commit/rollback and stopping/retry boundaries.
- [x] Run the combined backend suite sequentially, review test quality and verify unchanged production code or record any proven defect repair.
- [x] Update test inventory/results and state with remaining integration/coverage limits.

## Execution and verification
Use independent file ownership under subagent-driven-development: access/read test file, API contract test file, coordinator batch transaction test file/docs. No concurrent Maven executions against shared target. Tests mock upstream contracts and run without database/broker/real auth. Transaction tests verify orchestration with an observable transaction manager, not PostgreSQL durability or concurrency. Coverage percentage is not claimed without an actual measured report. Existing completed implementation remains uncommitted; this task does not authorize discarding it.

## Initial execution evidence
- First targeted run passes 104 newly added cases: access/read63, API 35, batch 6.
- Independent read-only review found no weak or incorrect assertions in the three new files. Transaction fake handles suspension/resumption and is explicitly limited to orchestration evidence.
- A final write-entrypoint matrix is being added to ensure each mutation enforces role/action permissions, complementing the read audience matrix.

## Final results
| Added test class | Executed cases | Coverage purpose |
|---|---:|---|
| AcademicsAccessAndReadTest | 99 | Four-role read audience matrix, nine mutation entrypoints' role/action guards, current actor/scope, DTOs, summaries, candidates and safe failures |
| AcademicsApiContractTest | 35 | Request mapping, response JSON, server principal forwarding, validation, immutable field rejection and safe HTTP error contracts |
| AcademicsBatchTransactionTest | 6 | Prior commit/later rollback, retry without duplicate enrollment/event, invalid rows, audit failure, revoked write authority and REQUIRES_NEW under ambient caller rollback |
| Total added | 140 | Backend unit/controller tests only |

- Clean complete command: installed Maven `-o -q -f backend/pom.xml clean test`.
- Full backend: 304 tests across 36 suites; 0 failures, 0 errors, 0 skips. U05 backend: 210 executed cases (previous 70 + added 140).
- New classes: access/read 99, API 35, batch 6; all pass. Existing architecture and regression tests pass in the same clean run.
- Independent test-quality review found no actionable issues; scoped review of the final 36 mutation matrix cases confirmed checks cannot pass for a wrong denial reason and verify no writes/events.
- SHA256 matches for all 449 production files saved before this task; no source/dependency/runtime behavior changes. Existing uncommitted U05 implementation preserved. Only tests and documentation added/updated for this request.
- Frontend code/tests unchanged; previous frontend verification is not counted as a new run in this task. Previously recorded 36 U05 frontend cases / 70 whole-project cases remain distinct from backend counts.
- No numeric coverage percentage or exhaustive branch claim. MockMvc standalone does not prove real sessions/security filters. The observable transaction fake proves service orchestration, not real PostgreSQL locking/durability. Schema, real authentication/audit and concurrency still require cross-unit/integration verification.
- No commit, push, deployment or real business data writes.
