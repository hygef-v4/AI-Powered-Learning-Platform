# MCP staff expansion — implementation and verification

Date: 2026-10-26. Scope: requirements R01–R13 and the approved seven-task code generation plan. Source implementation is complete; final verification results are recorded below. Real-account acceptance of the newly added tools remains separate.

## Delivered behavior

- ACTIVE Teacher, Subject Manager and Admin can use MCP. Authentication refreshes current role/status for every bearer request. Student credentials fail authentication; Student retains personal AI and Practice in the app without requesting MCP credentials.
- Teacher/Manager discover assigned classes, subjects, assignments, quizzes and materials through existing authorized domain services. Lists use zero-based page/size, default 20 and maximum 100. Selecting a class and subject simultaneously is rejected. Materials support bounded version-aware continuation without splitting Unicode characters.
- Assignment/quiz creation uses existing authoring/validation and creates a DRAFT plus a separate READY proposal. No generated content is automatically applied, published, submitted, or sent to a provider. Assignment creation returns a reviewUrl that opens the existing proposal dialog without generating again. Quiz proposals use the existing quiz review list.
- Creation retry uses a persistent unique account/operation/requestRef key, canonical payload fingerprint and transaction-scoped PostgreSQL advisory lock. Mutation and stored ID response share a transaction. Repeating identical requests returns the same IDs; changed payload conflicts. Every domain caller reauthorizes before replay. The stored response describes the original creation; reread the draft for its current version after human edits.
- Manager question bank supports scoped search, schema context, optional questionId for full question/answer context, and MCP proposals using the existing BANK review/acceptance lifecycle. Mixed subject/question IDs are rejected.
- Grading supports individual ATTEMPT and GROUP_DOCUMENT roots, submission discovery/filtering, current evaluation version/hash, rubric snapshot, structured document blocks and the existing dynamic output schema. MEMBER evaluations are rejected. Continuations bind evaluation/version/hash/rubric/document fingerprint; changed snapshots require starting again. A single oversized block returns an error requiring manual review.
- Read/write batches support 1–20 items. Each item has index/outcome/result or a safe error code. Failed/duplicate items do not discard valid results. Each proposal uses the existing recordMcp transaction and retry checks. Read results stop at an explicit aggregate budget; retry limit errors separately. No official grade is saved or published.
- Teacher summaries report authorized submission/grading counts, exact-text frequencies from existing structured feedback and explicit coverage. Assignment summaries inspect at most 100 rows. Class summaries inspect at most 10 assignments and 20 rows each; PARTIAL and coverage fields identify omitted data. This is deterministic aggregation, not a new AI assessment. Distribution uses published grades from GradebookService.
- Admin sees only capabilities and four read-only reporting tools: AI usage (7/30 days), paginated quotas, masked institutional connection DTOs and the existing dashboard with dependency availability sections. Admin MCP UI is `/admin/mcp`, guarded by the Admin layout and independent of personal AI endpoints.
- Protocol keeps the existing versions, bearer transport and rate limit. Input remains at most 64 KB. Oversized results return MCP_RESULT_TOO_LARGE instead of successful truncation. Serialized envelope size is checked in addition to the 192 KB content budget. Non-permission domain errors expose machine-readable codes; 403/404 remain masked.

## Migration and runtime boundary

New additive migration: `backend/src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql`. It creates only retry metadata/ID response storage, with SELECT/INSERT for the existing app database role. Historical migrations were not edited.

The migration, app-role privilege, concurrency, conflicting replay and rollback were checked in disposable Testcontainers PostgreSQL. This session did not run that migration against the user's live database, restart the live stack, change provider configuration, call a live provider, write live grades, commit, push or deploy. Until the local backend is rebuilt/restarted with its normal Flyway configuration, the running server may still expose the previous tool catalog.

## Verification ledger

| Check | Actual evidence |
|---|---|
| Initial backend baseline | 35 tests passed for inherited admission/material/discovery code |
| Initial UI regression | Student MCP endpoint/UI regression failed first, then passed after role-aware loading |
| Creation transaction | PostgreSQL concurrent duplicate/rollback tests passed; missing app-role grant was reproduced and fixed |
| Domain creation/review | Class and subject assignment/quiz creation tests; containers remain DRAFT/empty and proposals READY; no provider dispatch |
| Protocol catalog | Real tool schemas compile; every role × READ_ONLY/READ_WRITE catalog validated. A fake provider exercises initialize/list/call for every visible tool using schema-derived inputs; this is protocol evidence, not real resource/provider acceptance |
| New safety regressions | Changed snapshot, oversized block/batch/result, duplicate/stale batch item, mixed bank IDs, assignment review link and stale rubric replay covered |
| Full backend verify | Final run: BUILD SUCCESS; 1438 tests, 1434 passed, 4 environment-gated skips, 0 failures/errors; jar repackaged; ArchitectureTest passed |
| Full frontend | 91 files / 560 tests passed, zero failures; jsdom emitted its navigation-not-implemented notice |
| Frontend lint | Passed with zero errors/warnings |
| Frontend typecheck/build | Final Next production build passed, including TypeScript and `/admin/mcp`; standalone typecheck rerun after correcting the test fixture |
| Diagnostic fixture | `python scripts/test-check-mcp.py`: 2 test methods passed (protocol success/negative cases) |

Earlier red runs intentionally demonstrated missing behavior. One new summary-test fixture initially lacked the old page-size stub; it was corrected and rerun to show an assertion failure for missing aggregation before implementation. A new UI fixture initially missed the required businessContext type field; that fixture was corrected. Windows JVM/pagefile allocation failed at first; constrained heap, SerialGC and two active processors allowed compilation and verification without stopping unrelated applications.

## Reproduce build and tests

From the repository root, use the existing Java/Maven installation and dependency cache:

```powershell
$env:MAVEN_OPTS='-Xms16m -Xmx384m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=48m -XX:ActiveProcessorCount=2'
mvn -f backend/pom.xml '-DargLine=-Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2' verify
npm --prefix frontend test -- --maxWorkers=2
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend run build
python scripts/test-check-mcp.py
```

Maven is not on this machine's PATH; the actual run used the existing `apache-maven-3.9.12` wrapper-cache executable and `-Dmaven.repo.local` pointing to the existing user cache. No dependency version was changed. Run frontend tests and the production build sequentially on a constrained machine.

Expected outputs: Maven BUILD SUCCESS and repackaged jar; Vitest zero failures; ESLint/TypeScript exit zero; Next build includes `/admin/mcp`. Testcontainers must have Docker access. Four older environment-gated integration cases require separate U06/U12 test JDBC variables and are reported as skipped, not passed. Do not point these variables at a live database.

## Review and rulings

The final review used the code-reviewer checklist as a separate self-review; execution remained sequential without subagents as the plan required. ArchitectureTest verifies module boundaries. Author self-review provides less independence than a separate reviewer; no merge/publication was performed.

- Existing admission/discovery work was already present on resume. It was preserved and validated; no historical RED evidence is invented for those inherited edits.
- SQL retry persistence is encapsulated by the U06 transaction service instead of a separate repository class. The public IMcpWriteRequest contract and transaction boundary remain as planned. Cost if wrong: a future storage refactor touches that service, not domain callers.
- Student/Admin UI dependency repair was completed before the remaining authoring/grading work. This avoids leaving Student UI dependent on a forbidden endpoint. All domain work remains within the same agreed scope.
- Assignment proposals need an explicit review link because the prior editor only retained proposal IDs in local state. Added reviewUrl plus loading of that proposal, preserving the human apply step; no automatic apply.
- Reports deliberately bound coverage and declare PARTIAL. A complete large-class report requires narrower assignment calls or a later reporting-pagination extension. Individual grading contexts and batch items use their documented continuations instead.

No known critical/important review finding remains after the verified fixes. Live account acceptance and performance/load certification were not substituted by mocks. No throughput/SLA claim is made.

Final full backend run finished at 2026-10-26T17:56:22+07:00. The four skips are two AiRepositoriesIntegrationTest cases without U06_TEST_JDBC_URL and two GroupRepositoriesIntegrationTest cases without U12_TEST_JDBC_URL. New McpWriteRequestIntegrationTest cases ran against isolated PostgreSQL and were not skipped. Full frontend evidence is 91 files / 560 passed; the new proposal-dialog test was additionally rerun after its type-only fixture correction. Logs reside under `.tmp/mcp-staff-*`, with focused RED/GREEN evidence under `.tmp/mcp-*-red.log` / `.tmp/mcp-*-green.log`; logs are not publication artifacts.

## Extension compliance

Applicable SECURITY-03/04/05/08/09/12/15: inherited role/resource checks, masked secrets and permission errors, validated/bounded untrusted input/output, least-privilege migration, protected UI and isolated regression tests retained. Applicable RESILIENCY-04/06/10: atomic retries, independent batch outcomes, explicit continuation/partial/unavailable states, safe errors and repeatable build instructions. Other rules are N/A per the project's recorded extension scope. Property-based extension remains disabled.

See [manual acceptance guide](2026-10-26-mcp-staff-expansion-manual-test-guide.md) and [setup/tool guide](../../../docs/guides/mcp-clients.md).

## Local runtime smoke test — 2026-10-26

Restarted the local backend and frontend from the current workspace. Flyway applied `20261026.1800 - create mcp write requests`. The Teacher token passed initialize, ping, tools/list and `get_my_capabilities` through `http://localhost:3000/mcp`: role TEACHER, access READ_WRITE, 23 visible tools. Read-only `list_my_classes`, `list_my_subjects` and class-scoped `list_assignments` succeeded (1 class, 1 subject, 2 assignments at test time). Full acceptance remains pending for READ_ONLY, out-of-scope IDs, authoring/grading writes and Manager/Admin roles.

### Quiz authoring via MCP

On 2026-10-26, `create_quiz_draft` created **MCP QA - Software Requirement - 2026-10-26 182436** in the Teacher's U05-A test class. Quiz `84c5f170-b3ab-43e4-9a73-dcc3c4468c3a` is DRAFT version 1; proposal `6bf3e5d2-005a-44c4-8dd5-9e7f993837e8` is READY with two sample questions. A same-payload/requestRef retry returned the same IDs and the draft list contains one matching quiz. `read_quiz_context` confirms zero active questions, so the proposal is still awaiting human review. No provider call, publication, or student assignment occurred. UI review/apply and settings/material cases remain pending.

### Grading discovery via MCP

Teacher `list_assignments` and `list_submissions` succeeded within U05-A. The QA assignment is still DRAFT and has one NOT_SUBMITTED row with no evaluationId. The other assignment has one PUBLISHED evaluation. No student submission content was read and no grading proposal was written. The proposal endpoint accepts only PENDING/DRAFT evaluations, so full grading context and proposal-write acceptance awaits a QA student submission in an eligible state.
