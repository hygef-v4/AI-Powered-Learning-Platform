# MCP Staff Expansion Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task in the current session. Execute sequentially without subagents. Steps use checkbox syntax for tracking.

**Goal:** Implement the agreed staff-only MCP authoring, discovery, grading and reporting workflows.

**Architecture:** Domain modules own their MCP providers and resource checks, using existing application services and public contracts. U06 owns bearer authentication, tool filtering, bounded protocol handling and an idempotent-write contract. Admin reporting stays separate from academic authoring/grading.

**Tech Stack:** Java 17/Spring Boot 3.5.6, PostgreSQL/Flyway, Next.js/React/TypeScript, JUnit/Mockito and Vitest.

**Spec:** `../../../inception/requirements/2026-10-26-mcp-staff-expansion.md`.

**Status:** User approved continuation with "làm tiếp" on 2026-10-26. Resuming implementation sequentially in the existing dirty workspace; no commits, push or live migration.

## Global constraints

- Only ACTIVE Teacher, Subject Manager and Admin accounts may use MCP. Student personal AI stays available through the app.
- Teacher/Manager resource access follows current class/subject assignments; Admin does not receive academic permissions.
- Protocol remains Streamable HTTP with current supported protocol versions, bearer headers and no JSON-RPC batch.
- New tools do not call a provider; external agents generate content and the platform validates/stores drafts/proposals.
- Preserve current unpublished-draft and human-review/publication workflows.
- Existing dirty workspace files belong to the user/session and must be preserved. No automatic commit, push, deployment or provider request.
- Before frontend edits read applicable local Next documentation as frontend/AGENTS.md requires.
- Application code belongs under backend/frontend, documentation under aidlc-docs/docs, never code inside aidlc-docs.

## Review focus

1. Student tokens issued before this change, and a staff account subsequently changed to Student, must fail on the next request (Task 1).
2. Idempotency replay after ownership/role changes must recheck access; concurrent identical creates must produce one draft (Task 3).
3. Large documents, rubric changes and group submissions must never produce an apparently complete but truncated grading context (Task 5).
4. A batch mixing unauthorized, stale, duplicate and valid items must preserve valid outcomes without leaking other-class details (Task 5).
5. Student connection-page loading must not depend on the now-forbidden MCP endpoint; Admin creation must not depend on AI_PERSONAL (Tasks 1 and 6).

## Task 1: MCP admission, actions and Student removal — R01–R03, R12

**Modify:**
- `backend/src/main/java/edu/aiplatform/identity/contract/Actions.java`
- `backend/src/main/java/edu/aiplatform/identity/application/AccessPolicy.java`
- `backend/src/main/java/edu/aiplatform/ai/application/McpCredentialService.java`
- `backend/src/main/java/edu/aiplatform/ai/application/McpToolRegistry.java`
- Existing AssignmentMcpTools, AssignmentWorkMcpTools and MaterialMcpTools in their domain application packages.

**Tests:** McpCredentialServiceTest, McpServerTest, AiControllersTest, AssignmentMcpToolsTest and relevant work/material MCP tests under backend/src/test/java/edu/aiplatform.

**Interfaces:** Add `Actions.MCP_SELF`; credential endpoints keep the existing REST shape and use this action. AGENT_ROLES becomes TEACHER/SUBJECT_MANAGER/ADMIN. Tool discovery filters the three roles, and registry exposes no Student tools even if called directly with a Student role. Existing PracticeFeedbackService UI methods remain intact.

- [x] 1.1 Add admission tests for create/list/revoke, active old Student bearer, staff-to-Student account changes, Admin bearer, expiry/revocation and READ_ONLY visibility.
- [x] 1.2 Run the affected backend tests and establish failures for changed permissions.
- [x] 1.3 Add MCP_SELF policy, update credentials/authentication, remove Student from domain definitions and remove registration/dispatch of read_own_work and record_private_feedback.
- [x] 1.4 Verify Student retains AI_PERSONAL and PRACTICE_FEEDBACK UI access while Admin has MCP_SELF without AI_PERSONAL or academic actions; run affected tests (35 baseline tests).

## Task 2: Scoped discovery, constraints and material paging — R04

**Create:**
- `backend/src/main/java/edu/aiplatform/ai/application/McpCapabilitiesTools.java`
- Domain providers: `academics/application/AcademicsMcpTools.java`, `subjects/application/SubjectMcpTools.java`.
- Matching unit tests under backend/src/test/java/edu/aiplatform.

**Modify:** AssignmentMcpTools/AssignmentAuthoringService, QuizMcpTools/QuizAuthoringService, MaterialMcpTools/MaterialQueryService; extend domain contracts only where current public queries cannot express the required authorized list.

**Interfaces:** Implement get_my_capabilities, list_my_classes, list_my_subjects, list_assignments, list_quizzes, list_materials and read_authoring_requirements as specified. List results use `items,total,page,size` (page 0, size 20 default/100 max). Material response retains existing fields and adds `offset,nextOffset,complete`; optional expectedVersion rejects changed material. No Admin academic discovery.

- [x] 2.1 Add scope/paging/filter tests: Teacher assigned class, Manager managed subject, unrelated class/mismatched class-subject, inactive scope/material and invalid paging.
- [x] 2.2 Add no-leak tests for totals and discovery; test capability visibility for all roles and token access levels.
- [x] 2.3 Implement lists with existing authorized services, not unrestricted queries; derive authoring schemas/constraints from current validators rather than duplicated constants.
- [x] 2.4 Extend authorized material reads with bounded version-aware continuation; verify legacy materialId-only calls, Unicode boundaries and changes between pages.
- [x] 2.5 Run changed-domain tests and schema validation for every new READ tool.

## Task 3: Idempotent creation of assignment and quiz — R05–R06

**Create:**
- `backend/src/main/java/edu/aiplatform/ai/contract/IMcpWriteRequest.java`
- `backend/src/main/java/edu/aiplatform/ai/application/McpWriteRequestService.java`
- `backend/src/main/java/edu/aiplatform/ai/domain/McpWriteRequestRepository.java`
- `backend/src/main/resources/db/migration/ai/V20261026_1800__create_mcp_write_requests.sql` (confirm version is unused before creation).
- Domain `AssignmentMcpAuthoringService.java` and `QuizMcpAuthoringService.java` in their application packages; corresponding tests and idempotency persistence integration test.

**Modify:** Existing assignment/quiz MCP provider definitions and existing authoring/draft services only where a reusable atomic domain operation is needed.

**Interfaces:** `IMcpWriteRequest.execute(Actor actor, String operation, String requestRef, String fingerprint, Supplier<Map<String,Object>> mutation)` returns stored ID response or executes one mutation. Database unique key `(account_id,operation,request_ref)`, canonical semantic fingerprint, safe response JSON and timestamps; record and creation/proposal in one transaction. Caller rechecks scope before replay. Tool input follows the spec, output is IDs/version/status without full generated content.

- [x] 3.1 Add tests for concurrent duplicate requestRef, conflicting payload, mutation rollback and retry (2 PostgreSQL tests passed); caller permission-loss regressions added.
- [x] 3.2 Add authoring tests for class/subject targets, invalid rubric/question totals, invalid settings/material scope, archived containers and unsupported group mode.
- [x] 3.3 Implement idempotency persistence/transaction contract and additive migration without modifying historical migrations or live data. Ruling: SQL is encapsulated in U06's transaction service rather than a separate repository; the atomic public contract is unchanged.
- [x] 3.4 Implement create_assignment_draft through existing authoring and saveFromAgent validators, returning actual DRAFT plus reviewable proposal.
- [x] 3.5 Implement create_quiz_draft through existing authoring/settings and saveFromMcp validators, returning actual DRAFT plus reviewable question proposal.
- [x] 3.6 Verify no publish/provider/email side effect, the existing UI can inspect/apply proposals, invalid creation rolls back, and all affected tests pass.

## Task 4: Subject Manager question bank — R07

**Create:** `backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankMcpTools.java` and its unit tests.

**Modify:** QuizAiDraftService to expose a bank-target MCP proposal operation using existing bank draft validation/provenance and acceptance behavior; IQuizAiDraftRepository/JdbcQuizAiDraftRepository only if existing bank persistence needs a new insertion path.

**Interfaces:** read_question_bank_context(subjectId), list_bank_questions(subjectId, filters, page, size), save_question_bank_draft(subjectId, questions, requestRef, agentLabel). Store BANK proposal with MCP provenance; do not insert active bank questions directly. Reuse IMcpWriteRequest for safe retry.

- [x] 4.1 Add tests for Manager-owned subject, foreign subject, Teacher/Student/Admin denial, READ_ONLY write denial and duplicate requestRef.
- [x] 4.2 Implement read/search/bank proposal operations using QUESTION_BANK_VIEW/MANAGE, existing question schema and active-subject checks.
- [x] 4.3 Verify proposals appear in the existing bank review flow and accepted questions retain the existing draft lifecycle; run quiz MCP/bank tests.

## Task 5: Complete grading context, batch and teaching summaries — R08–R10

**Create:** `backend/src/main/java/edu/aiplatform/grading/application/GradingMcpQueryService.java`, `GradingMcpBatchService.java` and tests.

**Modify:** GradingMcpTools, SubmissionReviewService/GradingProposalService/GradebookService query adapters where needed, and MCP oversized-result handling/tests if new bounded results expose existing unsafe truncation.

**Interfaces:** Add list_submissions, read_grading_context, read_grading_batch, record_grading_proposals_batch, list_grading_proposals, read_class_learning_summary, read_grade_distribution. Grading context includes `evaluationId,expectedVersion,submissionRef,submissionHash,rubricSnapshot,documentBlocks,outputSchema,complete,continuation`. Source content comes from authorized review detail and evaluation snapshots. Typed per-item batch result includes input reference, OK/error code and optional proposalId/context; max 20 items, request body remains <=64 KB.

- [x] 5.1 Add tests for individual ATTEMPT and GROUP_DOCUMENT root, member evaluation rejection, foreign class and changed rubric/hash/version.
- [x] 5.2 Add multi-page large-content tests preserving snapshot across pages; validate rubric/context completeness and structured block references without relying on plain-text flattening.
- [x] 5.3 Implement scoped submission discovery and grading context; supply schema from the existing grading prompt/validator contract and current evaluation version.
- [x] 5.4 Implement bounded batch reads/continuations under 192 KB; return explicit too-large/continuation state rather than a successful truncated context.
- [x] 5.5 Implement per-item batch writes using existing recordMcp and independent transactions; retain existing idempotency and safe partial failure semantics.
- [x] 5.6 Add mixed batch tests for valid/unauthorized/stale/duplicate/invalid proposal, retry and partial success; verify no official grade write or publication occurs.
- [x] 5.7 Implement proposal status reads and class summaries/distribution from current authorized services; use explicit calculatedAt/source/status and test empty/partial data.
- [x] 5.8 Run grading service/validator/MCP/batch tests and changed protocol tests.

## Task 6: Admin reports and role-aware UI — R11–R12

**Create:**
- `backend/src/main/java/edu/aiplatform/ai/application/AdminAiMcpTools.java`
- `backend/src/main/java/edu/aiplatform/admindashboard/application/AdminDashboardMcpTools.java`
- `frontend/src/features/ai/admin/AdminMcpPage.tsx`
- `frontend/src/app/admin/mcp/page.tsx`
- Corresponding provider/UI tests.

**Modify:** AiConnectionPage.tsx, McpAgentSection.tsx (reuse without personal API dependency), role-aware identity shell navigation, relevant exports, and existing AiConnectionPage.test.tsx. Review frontend/AGENTS.md and local Next guides before these changes.

**Interfaces:** Admin tools read_ai_usage_summary, read_ai_quota_policy, read_ai_connection_status, read_platform_health_summary use existing admin-authorized service APIs and masked DTOs. Admin route is protected by existing Admin shell and uses existing MCP credential REST endpoints; default token access READ_ONLY. Student AiConnectionPage loads personal connections/providers/sources without requesting MCP credentials or rendering MCP summary/setup sections.

- [x] 6.1 Add Admin report tests for role denial, safe provider DTOs, bounded teacher quota lists and unavailable dependency sections.
- [x] 6.2 Implement read-only Admin providers and verify zero academic tool exposure for Admin and zero Admin tool exposure for Teacher/Manager.
- [x] 6.3 Add UI tests asserting Student page remains usable and makes no MCP-credential request; Teacher retains create/one-time token display (9 tests passed across 2 files).
- [x] 6.4 Implement protected Admin MCP page/navigation using shared credential components, without fetching personal connections/sources. Ruling: UI admission work done before remaining domain tools to repair the Student page dependency immediately.
- [x] 6.5 Run changed frontend tests/typecheck/lint; verify Admin page access guards, API failures and one-time token handling.

## Task 7: Integration, documentation and handoff — R13

**Modify:** docs/guides/mcp-clients.md, docs/guides/ai-api-keys.md and relevant API contracts; scripts/check-mcp.ps1 only if new discovery output requires compatibility work.

**Create:** `aidlc-docs/construction/build-and-test/2026-10-26-mcp-staff-expansion-results.md` with build/check instructions and actual results.

- [x] 7.1 Run focused backend tests per task; finish with `mvn -f backend/pom.xml verify` using the repo's available Java/Maven or documented Docker build route. Integration database must be a verified test database/container.
- [x] 7.2 Run `npm --prefix frontend test --` with changed test paths, frontend typecheck/lint and production build through existing package scripts; expand failures only as needed.
- [x] 7.3 Run fake-provider/fixture protocol checks: initialize, tools/list and tools/call for every role/token combination, schema compatibility, expiry/revocation, validation, 64 KB input and 256 KB output bounds.
- [x] 7.4 Validate additive migration and idempotency concurrency in a test database. Do not migrate/restart the user's live stack as a side effect of tests.
- [x] 7.5 Update setup/role/tool documentation to remove Student MCP and explain Admin setup, create-vs-proposal behavior and bulk continuation/retry.
- [x] 7.6 Review diff for preserved prior changes, secrets, cross-module boundaries and requirement coverage. Record actual test counts, failures and remaining runtime acceptance; do not claim unexecuted real-account/provider tests.
- [x] 7.7 Update every completed checkbox, audit/state and final handoff. No automatic git commit/push/deployment; separate runtime acceptance or publication actions follow user authorization.

## Verification commands and expected outcome

- Backend focused tests: Maven `-Dtest=<affected test classes> test`; expected zero failures/errors. Reuse current repo Docker Maven route if host tooling is unavailable.
- Backend complete check: `mvn -f backend/pom.xml verify`; expected BUILD SUCCESS or explicitly reported environment/dependency blocker.
- Frontend: `npm --prefix frontend run typecheck`, `npm --prefix frontend run lint`, relevant `npm --prefix frontend test -- <paths>`, `npm --prefix frontend run build`; expected successful exits.
- Diagnostic fixture: `python scripts/test-check-mcp.py`; expected supported PowerShell fixture scenarios pass without accounts or provider keys.
- Real MCP writes, provider calls and live DB migration are not substituted for isolated tests. Any runtime acceptance performed later must state its account/resource scope and resulting mutations.

## Approval and stage tracking

- [x] Existing workspace and accepted role/scope decisions inspected.
- [x] Requirements, stories, proposed contracts and implementation sequence recorded.
- [x] Plan self-review mapped R01–R13 to Tasks 1–7 and all five review concerns to tests.
- [x] User explicitly approves this technical plan ("làm tiếp", 2026-10-26).
- [x] Execute Tasks 1–7; sequential execution and the documented early UI repair deviation are complete.

Approval file: `../../../inception/requirements/2026-10-26-mcp-staff-expansion-approval.md`.

## Execution evidence and deviations — 2026-10-26
Tasks 1–6 implementation and domain verification complete. Existing admission/discovery edits were inherited and validated (no historical RED claim). Full source verification passed with 1434 tests / 4 environment skips before the final rubric-replay guard; the final full rerun is in progress. The role/token catalog and fake-provider protocol calls for every visible tool passed; class and subject create/review checks passed (47 focused tests). Source self-review fixed review-link availability, mixed bank IDs, explicit summary coverage and stale-rubric replay. Full frontend 560/560 passed. Detailed coverage and runtime limits are in the staff expansion results report; real-account/provider acceptance is not marked complete.


Final automated results: backend verify BUILD SUCCESS, 1438 total / 1434 passed / 4 environment-gated skips / zero failures or errors; frontend 91 files / 560 passed, lint, production build and TypeScript pass. Diagnostic fixture 2/2 test methods pass. Final review: self-review, consistent with sequential/no-subagent execution. No deferred code finding; bounded reports and oversized-block manual review are explicit product limits. Real-account acceptance remains pending in the manual guide. No commit/push/live migration/deployment.
