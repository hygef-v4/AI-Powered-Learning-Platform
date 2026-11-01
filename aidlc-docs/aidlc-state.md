# AI-DLC State Tracking

## U06/U12 review feedback — 2026-11-02

Triage of the 2026-11-02 review feedback for the units Võ Minh Tài owns (U02, U06, U12); Quiz, Assignment, Grading and materials items belong to U07–U11/U13 owners and were not changed. Implemented: U06 default AI source (BR-U06-28); U12 take-over editing, leader finalize, section history with restore, image upload, DOCX/Draw.io import mapped by section title without AI, peer review (BR-U12-33, 35, 39, 46, 47); user guide `docs/guides/user-guide-audit-ai-group.md`. Peer scores are informational; weighting into the final grade (document/peer/presentation) needs U07/U09. Bilingual UI not started (cross-unit decision). [Plan](construction/plans/code-generation-plan/2026-11-02-u06-u12-feedback.md), code summaries U06/U12. Branch `feat/u06-u12-feedback-2026-11-02`.

## Server-managed MCP workflows — 2026-11-01

Implemented full workflow delivery via read_agent_workflow (AUTHOR_ASSIGNMENT/GRADE_WITH_RUBRIC), role-filtered READ access and deterministic content versions. Full source lives in backend classpath resources; local skills are optional wrappers, no user download required. Capabilities/requirements guide discovery; QUIZ/BANK remain separate. Current U06/U07/U09 rules and guides synchronized. [Requirements](inception/requirements/2026-11-01-server-agent-workflows.md), [plan](construction/plans/code-generation-plan/2026-11-01-server-agent-workflows.md), [evidence/limits](construction/build-and-test/2026-11-01-server-agent-workflows-results.md). Backend focused/architecture: 39 tests passed, zero skipped; hook checks 14 passed; package and required jar resource checks passed. Existing MCP capability call advertises new tool; direct authenticated RPC fetched both complete workflows without local skills. Backend reloaded; clients with stale tool metadata need refresh/new session. No provider call, assignment/grade write, private config installation, commit or push.

## External-agent workflow — 2026-11-01

Implemented the approved agent-first flow: conversation/clarifications in the external agent, exact UI review links, contiguous current-context read enforcement in Redis before MCP grading writes, and optional skill guidance available through capabilities/requirements. No mandatory platform initiation form or session table. Current U06/U07/U09 business rules synchronized. [Requirements](inception/requirements/2026-11-01-external-agent-workflow.md), [plan](construction/plans/code-generation-plan/2026-11-01-external-agent-workflow.md), [results/limits](construction/build-and-test/2026-11-01-external-agent-workflow-results.md). Backend affected/architecture: 67 passed, zero skipped; frontend affected: 38 passed; build/typecheck passed; lint zero errors/two existing warnings. Real Redis script and existing older proposal link checked through Orca; backend restarted, health UP. No real model quality or all-client acceptance claim; no global install, commit or push.

## MCP quality remediation — 2026-11-01

Latest setup scope: only Codex, Claude Code, OpenCode and Antigravity; user requested removing Gemini CLI. Codex is now the default in both guide and token dialog. Gemini API provider unchanged. Focused 18 tests and typecheck passed; no external config removal.

Setup follow-up: added copyable English agent setup prompt and preview in the shared guide/token dialog, preserving client selection and other servers. Token is included only in the explicit token dialog, with disclosure. Focused 18 tests, typecheck, targeted lint and production build passed; Orca guide checked. No real client setup or secret copy performed.

Implemented strict complete AI proposal validation with legacy replay compatibility, shared API/MCP assignment context and policy, metadata-only discovery, server-sourced rubric limit warnings, four-client application/file-based setup and two repository-local skills with optional Gemini hook adapters. User requested no terminal setup instructions; guide follows that preference. [Plan](construction/plans/code-generation-plan/2026-11-01-mcp-quality-remediation.md), [results and limits](construction/build-and-test/2026-11-01-mcp-quality-remediation-results.md). Backend 1,630 runtime cases plus 2 separately rerun architecture cases passed; frontend full 644 plus final targeted setup regressions passed. No global installation, provider call, new token, commit or push. Actual teacher-scored quality, authenticated acceptance in all external clients and load latency remain unmeasured. Existing security/resiliency scope retained; PBT disabled.

## Back navigation and page selections — 2026-11-01

Implemented the user's request for Back to restore the actual previous page/tab. Shared history-aware links retain fallback destinations and navigation guards; page tabs and relevant selections/filters live in URL parameters. Next native-history integration verified after correcting the internal-state bypass. Full frontend: 641 tests/96 files passed; build/typecheck passed; lint zero errors/two existing warnings. Orca verified grading → submissions → Back restores tab/student/filter, and same-page tab Back/Forward restores the visible tab. [Plan](construction/plans/code-generation-plan/2026-11-01-back-navigation-state.md), [results/limits](construction/build-and-test/2026-11-01-back-navigation-state-results.md). No backend/data changes, commit or push.

## Neutral white palette follow-up — 2026-11-01

User found the Slate Blue light surfaces cold and requested a white background matching the supplied reference. Light canvas is now #FAFAFA; sidebar/topbar/cards white; text, borders, sunken surfaces and shadows neutral gray. Blue remains the control/selection accent; dark palette unchanged from the preceding trial. PRODUCT.md updated. Fourteen representative contrast pairs passed (minimum 4.75:1); Orca confirmed the actual canvas/sidebar colors and no horizontal overflow, and the rendered page was inspected. CSS-only follow-up; no new tests or behavior changes. No commit/push.

## Slate Blue palette trial — 2026-11-01

User requested trying Slate Blue after reporting discomfort with the warm theme. Shared light tokens now use cool neutral surfaces, white cards and muted blue accents; dark tokens use slate surfaces and pale blue accents. Removed the oversized shell watermark, softened shadows and aligned the wordmark badge/login seal with the theme. Small brush logo brand colors retained. PRODUCT.md records this as a trial. Production build/typecheck, two shared suites (9 tests) and targeted ESLint passed. Fourteen representative palette contrast pairs passed (minimum 4.67:1); Orca inspected light/dark and confirmed no horizontal overflow at 390 px. No backend/behavior changes, new dependencies, commit or push. Full visual/role coverage not rerun.

## Gradebook and toolbar bug fixes — 2026-11-01

Implemented: historical rubric object envelopes now read alongside current arrays without rewriting stored data; editor tools have visible enabled hover/focus feedback and pointer cursors; assignment header dropdown stacks above the toolbar. [Plan](construction/plans/code-generation-plan/2026-11-01-gradebook-toolbar-fixes.md) and [results/limits](construction/build-and-test/2026-11-01-gradebook-toolbar-fixes-results.md). Fresh backend assignment/grading: 175 passed/zero skipped; frontend focused: 77 passed; typecheck/targeted lint/build passed. Backend restarted; same gradebook API returns 200, and Orca confirmed grading/progress data and menu visibility. Existing pending changes preserved; no persisted data rewrite, commit or push.

## Assignment distribution visibility — 2026-11-01

Implemented: anonymous grade-distribution visibility moved from class management to the assigned teacher's assignment settings, separately for each class. Defaults off; existing score publication and privacy guards remain. [Execution plan](construction/plans/code-generation-plan/2026-11-01-assignment-distribution-visibility.md) and [fresh verification/limits](construction/build-and-test/2026-11-01-assignment-distribution-visibility-results.md). Backend: 174 distinct focused tests passed, including actual PostgreSQL races and architecture. Frontend: 615 tests passed; build/typecheck passed; lint zero errors/two existing warnings. Local backend restarted with additive migration; Orca verified saving and persistence, leaving the test preference off. Pending scoped UI changes preserved. Security/resiliency scope retained; PBT disabled. No commit or push.

## Scoped workspace UI — 2026-11-01

Grading follow-up: blank answer space grows to fit the corresponding rubric when desktop side comments are taller. Resize/font/content updates recalculate it; mobile removes the imposed min-height. Focused tests and lint passed; Orca measured matching desktop section/card heights.

Follow-up: widen the shared assignment authoring/grading workspaces as well; document body text is 16 px with 28 px line height, and wide paper uses smaller side padding. Grading now identifies actual students from all pages of the existing submissions API and offers searchable student navigation while preserving unsaved-work confirmation. Related tests: 155/155 pass; build/typecheck and focused ESLint pass. Orca confirmed the student name and search menu at 390 px, and the author paper expands to 950 px plus a 300 px rubric rail at 1600 px, with 16 px body text and no page overflow. No backend changes, commit or push.

After discarding the previous remediation, the user requested only a wider student answer workspace and inline AI grading review. Changes are limited to an opt-in full-width editor and student shell width, plus replacing the grading AI dialog with a page section. Verified: 49 focused tests pass; production build/typecheck and focused ESLint pass. Orca measured 1110 px paper at 1440 px viewport, no student page overflow at 390 px, and inline AI review with zero dialogs at 603 px. No provider calls, backend/data changes, commit or push.

## Font trial — 2026-11-01

At the user's request, Plus Jakarta Sans now replaces Be Vietnam Pro in the frontend as a reversible trial. Six official self-hosted WOFF2 subsets and their OFL license were added; unused Be Vietnam Pro assets were removed. Orca verified Vietnamese normal/bold/italic loading and no document overflow at 390/1280 px in both themes. Frontend production build and lint passed; two existing lint warnings remain. The user subsequently requested a commit; no push was requested.

## Development UI follow-up — 2026-11-01

Disabled the Next.js on-screen development indicator reported on the mobile assignment page. Orca no longer lists the Dev Tools controls; production build passed. Plus Jakarta Sans was researched as an alternative typeface, with the existing user-confirmed Be Vietnam Pro choice retained pending selection.

## UI remediation — 2026-11-01

User requested a plan and implementation for observed UI defects. [Execution plan](inception/plans/2026-11-01-ui-remediation.md) tracks the fixes and checks. Scope is frontend presentation based on the existing visual audit and four screenshots; no backend, schema, or AI provider changes.

Completed: 599 frontend tests pass; lint 0 errors/2 pre-existing warnings; typecheck and production build pass. Orca confirmed dropdown layering, no document-level horizontal scroll at 390 px in the inspected AI tabs, and the add-key dialog fits the 390 × 844 px viewport. Broader visual regression across every role and task-running AI states remains unchecked.

After the dark-mode select report, added theme foreground/background to native select options. Fresh production build and lint passed; the full test suite passed 599/599 with four workers. The default worker configuration timed out once on the subject detail mutation test in two runs; that test passed alone. Orca measured the corrected option CSS colors but cannot capture the native popup surface.

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

## Bug remediation and doc sync — 2026-11-01

Commit `1cd58c02` on local branch `fix/review-bugs-2026-11-01` fixes 26 review bugs plus the DOCX preview CSP issue. Business rules, frontend components, API inventory, contracts and decision register were synchronized to the new behavior; summary in [review-bugs remediation](construction/remediation/review-bugs-2026-11-01/README.md). Frontend lint/typecheck/605 tests/build passed; backend compiled and unit-tested without Maven (environment-caused failures only). `mvn verify` and Testcontainers suites not run; nothing pushed.
