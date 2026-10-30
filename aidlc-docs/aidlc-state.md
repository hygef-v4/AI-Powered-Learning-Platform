# AI-DLC State Tracking

## Current source/documentation baseline — 2026-10-30

- Project origin: Greenfield, started 2026-09-12; current task: package/file moves only, per the latest user clarification; original implementations retained.
- Current source: branch `aidlc-docs`; package/file relocation included in this commit, after documentation commit `11df6307`. Code baseline `5a76e428`; 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Earlier snapshots below remain historical evidence.
- Current scoped stage: move-only scope correction and fresh local Build and Test complete; current Construction documentation synchronized. Earlier remediation/doc sync preserved. Student MCP remains UC41; Admin MCP denied; no deployment. Work executed directly without subagents.
- Source-observed means code exists; full application acceptance, fresh CI/runtime/provider/security/performance/restore effectiveness remain pending.
- [Execution plan](inception/plans/2026-10-30-aidlc-doc-sync-plan.md), [traceability](inception/reverse-engineering/doc-code-traceability.md), [decision register](inception/reverse-engineering/decision-register.md).

## Unit Progress (current source snapshot)

| Unit | Current responsibility | Source | Fresh acceptance |
|---|---|---|---|
| U01 | Account & Access | [source-observed](construction/u01-account-and-access/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U02 | Audit | [source-observed](construction/u02-audit-job-and-outbox/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U03 | Job and Settings | [source-observed](construction/u03-job-and-settings/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U04 | Subject Administration | [source-observed](construction/u04-subject-administration/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U05 | Class & Enrollment | [source-observed](construction/u05-class-enrollment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U06 | AI Connections | [source-observed](construction/u06-ai-connections/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U07 | Assignment Authoring | [source-observed](construction/u07-assignment-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U08 | Assignment Work | [source-observed](construction/u08-assignment-work/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U09 | Assignment Grading | [source-observed](construction/u09-assignment-grading/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U10 | Quiz Authoring | [source-observed](construction/u10-quiz-authoring/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U11 | Quiz Practice | [source-observed](construction/u11-quiz-practice/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U12 | Group Assignment | [source-observed](construction/u12-group-assignment/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U13 | Reference Materials | [source-observed](construction/u13-reference-materials/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U14 | Announcements & Notifications | [source-observed](construction/u14-announcements-and-notifications/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |
| U15 | Admin Dashboard | [source-observed](construction/u15-admin-dashboard/code/as-built-baseline.md) | Pending; evidence scoped/date-specific |

## Extension Configuration

| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis; scope retained |
| Resiliency Baseline | Yes | Requirements Analysis; scope retained |
| Property-Based Testing | No | Requirements Analysis; unchanged |

Security scope: SECURITY-03/04/05/08/09/12/15. Resiliency scope: RESILIENCY-04/06/10. Other controls N/A outside approved project scope. Automated authorization/privacy/resiliency regressions passed within the approved remediation scope; this does not certify every runtime control. Key-reveal design delta remains explicit.

## Historical lifecycle and approvals

[Full pre-sync state, approvals, dated test counts and pending issues](history/aidlc-state-before-2026-10-30-sync.md). This archive preserves the previous content including date-specific implementation notes; stale unit names/statuses there are historical. Audit remains append-only in [audit.md](audit.md). No historical approval is converted into fresh full-UC acceptance.

## Documentation verification

- [x] Six synchronization tasks complete; code-derived deltas D01–D14 addressed, additional D15 key reveal recorded.
- [x] Current Markdown links, OpenAPI parse/local references/operation IDs/path parameters, draw.io XML/page consistency, 12 refreshed PNG exports and visual inspection.
- [x] Independent read-only review findings corrected; source application/config/DB unchanged.
- [ ] Fresh application/runtime/provider/security/performance/restore acceptance remains pending; no such success claimed.

[Sync report](construction/build-and-test/2026-10-30-documentation-sync-report.md).

## Rule/use-case/test audit — 2026-10-30

- [x] Read assertions/fixtures and code for all15units, with per-UC evidence/gap rows in three appendices.
- [x] Fresh backend1372pass/99skip; frontend597pass/94files after restoring locked dependencies. No source/manifest/lockfile changes.
- [x] Subsequent remediation ran all backend cases: 1,611 passed/0 skipped; required PostgreSQL guard passed. The earlier 1,372/99 audit run remains historical.
- [x] Approved U03 P1 fixes, false-positive fixtures, scoped missing assertions, current BR/UC drift and Student/Admin MCP correction implemented and verified. Broader acceptance gaps remain explicit in the remediation report.

[Consolidated audit](construction/build-and-test/2026-10-30-unit-test-business-rule-audit.md). Passing suites do not certify full business-rule/UC acceptance.

## Remediation closure — 2026-10-30

[Results and practical limits](construction/build-and-test/2026-10-30-remediation-student-mcp-results.md). Backend 1,611 pass/0 skip; frontend 599 pass/94 files; required DB guard, lint/typecheck/build passed. BF-06/BF-08 exports inspected. Migration tested in disposable databases only. No commit/push/live migration/deploy.

## Code structure review and refactor proposal — 2026-10-30

- User requested a whole-project review with `ponytail-audit` and `clean-code`, including AI-DLC, and a simple refactor plan. Scope is review/planning, not implementation.
- Source reviewed at HEAD `5a76e428`; working tree initially clean. [Structure audit](inception/reverse-engineering/2026-10-30-code-structure-audit.md) and [proposed refactor plan](inception/plans/2026-10-30-code-structure-refactor-plan.md) prepared for review. Preserve feature/unit ownership; normalize internal controller/service/repository/model and frontend API/model/components/pages/hooks.
- Fresh frontend: 599 passed across 94 files; lint exit 0 with two existing warnings; typecheck passed. Focused backend architecture run did not reach tests because the workspace offline dependency cache was incomplete. Earlier 1,611-pass evidence remains historical, not a fresh result of this review.
- No application/test/config/dependency/DB changes; no commit/push/deploy. Security/resiliency scope retained; PBT disabled. Refactor execution is proposed and pending review; no construction approval is inferred from “Làm tiếp” during the review.

## Withdrawn broader refactor snapshot — 2026-10-30

This section records the superseded working-tree implementation. All non-move edits below were undone after the latest user clarification; use the move-only result at the end of this document.

- Explicit approval: “Giúp tôi refactor lại dự án theo kế hoạch”. Executed directly without subagents in the existing checkout, preserving prior documentation work.
- All 15 backend units and 18 frontend features now follow the approved structure. Admin-only unreachable MCP implementations removed; architecture guard hardened; U05/document/editor/AI-draft responsibilities separated; U05 list lookups batched; frontend public imports stabilized.
- Fresh final backend `mvn clean verify`: 1,614 passed, 0 failures/errors/skips; required PostgreSQL guard passed. Frontend: 601 passed/94 files; lint/typecheck/production build passed. Public unit contracts, dependency versions/lockfiles, migrations and infrastructure config unchanged.
- Current source inventory, all 15 Construction as-built/logical-component documents, services and test guidance synchronized. Earlier reports remain dated evidence, not a fresh acceptance claim.
- [Results and limits](construction/build-and-test/2026-10-30-code-structure-refactor-results.md); [execution ledger](construction/plans/code-generation-plan/2026-10-30-code-structure-refactor-progress.md). No commit, push, deployment or live migration. Browser/provider/load/security/restore acceptance remains unverified.

## Current move-only result — 2026-10-30

User clarified that only moving files/packages is wanted. All original 1,501 source/resource files are retained; 1,066 relocated, with necessary package/import/path updates. Broader splits, cleanup, optimizations, new public barrels and extra tests were undone. Original class/test bodies are retained, with only ArchitectureTest layer names and required-suite FQNs adapted.

Fresh backend clean verify: 1,611 passed/0 skipped, required PostgreSQL guard passed. Frontend: 599 passed/94 files; typecheck/build passed; lint exit 0 with two existing unused-import warnings. No staged changes, commit, push, history rewrite or deployment.

[Current results and limits](construction/build-and-test/2026-10-30-package-moves-only-results.md). Security/resiliency scope unchanged; PBT remains disabled. Existing full runtime/provider/E2E acceptance gaps remain outside this move-only change.

## Fresh verification after git mv — 2026-10-30 17:11 +07:00

HEAD `11df6307` contains documentation only; code remains staged as 1,066 renames and 86 reference updates. Fresh backend clean verify: 1,611 tests passed, 0 failures/errors/skips, JAR built; required PostgreSQL suites passed. Frontend: 599 tests passed/94 files, production build/typecheck passed; lint exit 0 with two existing unused-import warnings. Logs: `.tmp/2026-10-30-after-gitmv-*.log`. No source fix, commit, push or deploy. Existing browser/provider/load/restore acceptance limits remain unchanged.

## Commit authorization — 2026-10-30 17:13 +07:00

User requested “giúp tôi commit”. Include the verified file/package relocation and latest verification/audit notes in this commit. Pre-commit comparison confirms all 1,501 original source/resource files retained, 908 Java bodies and 506 TypeScript/TSX ASTs unchanged except necessary package/import/reference updates. Backend/frontend source matches the tested tree; test/build evidence above applies. No push, deployment or history rewrite.
