# MCP Authoring and Rubric Grading Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans task by task after plan approval. Independent subagents require the user's execution choice; none are needed for this planning task. Steps use checkbox syntax.

**Goal:** Make MCP connection instructions usable and rubric proposals complete, traceable and efficient, with optional skills/hooks and a separate grading-quality pilot.

**Architecture:** Reuse current MCP providers, assignment contracts and grading services. Enforce rules in the backend; distribute common workflow instructions to optional client packages. Keep human approval of official grades.

**Tech Stack:** Java 17, Spring Boot, Jackson, PostgreSQL, Next.js, React, TypeScript, JUnit/Mockito, Vitest and existing Python/PowerShell diagnostics. No new dependencies by default.

**Spec:** [Proposed requirements](../../../inception/requirements/2026-11-01-mcp-quality-remediation.md).

**Status:** Approved execution completed for server safeguards, setup guidance and repository-local packages. Evidence and uncompleted acceptance gates: [results](../../build-and-test/2026-11-01-mcp-quality-remediation-results.md). Actual teacher-scored model pilot remains pending.

## Global constraints

- Student/Teacher/Subject Manager connect; Admin cannot. Student remains submitted-own-Practice only.
- MCP never calls the platform's model provider and never finalizes/publishes official grades.
- Preserve historical records and manual workflows. No DB rewrite/migration is expected.
- Keep 30 rubric items per single AI grading request; warn on larger rubrics without blocking manual authoring.
- Keep existing 64 KiB arguments, 192 KiB result content/256 KiB envelope and 20-item batch ceiling. Check UTF-8 bytes, not only item count.
- Do not record real tokens, raw student work or provider-authored sensitive text in diagnostic output.
- Packaging is repository-local. Installing global skills/hooks/configurations is a later explicit action.

## Review focus

1. New strict validation must not prevent history display, legacy request replay or normal manual grading (Task 1).
2. A submission with unchanged hash but changed assignment context must invalidate continuation (Task 2).
3. Direct API access, frontend proxy and agent-in-container localhost are different connection cases (Task 3).
4. Subject-wide assignments, group roots and removed teaching access must stay scoped during metadata discovery (Task 4).
5. A transport failure after a committed write must not cause duplicate proposals during client retry (Tasks 1 and 6).

## Sequence and deliverables

Task 1 -> Task 2 -> Task 3 -> Task 4 -> Task 5 -> Task 6 -> Task 7.

Tasks 1–5 deliver the repair. Task 6 packages optional assistance against those contracts. Task 7 measures model quality; it must not be reported as passing without teacher-provided reference grades. Plan approval precedes application implementation. Proposed commits below are logical boundaries, not authorization to commit automatically.

### Task 1: Reject incomplete new AI grading proposals — R01/R02

**Modify:**
- `backend/src/main/java/edu/aiplatform/grading/service/GradingProposalValidator.java`
- `backend/src/main/java/edu/aiplatform/grading/service/GradingProposalTransactions.java`
- `backend/src/main/java/edu/aiplatform/grading/service/GradingPromptBuilder.java` only where schema/runtime parity needs correction.

**Tests:** `GradingProposalValidatorTest`, `GradingProposalServiceTest`, `GradingProposalResultHandlerTest`, `GradingMcpReplayTest`, `GradingMcpToolsTest`, `GradingPromptSchemaTest` under `backend/src/test/java/edu/aiplatform/grading/service/`.

**Interfaces:** Preserve current `validate(JsonNode, List<RubricView>, Set<String>)` call sites; make new proposal validation require complete `itemScoresByRubric` and `structuredFeedback`. Read/render existing records without revalidating them as new writes. Exact replay can return the existing result only after current actor/scope/hash/version/state/request checks; altered content with the same requestRef still conflicts.

- [x] Add regressions: nonempty rubric plus `{checks:{},score:"0"}` rejects; missing/null structuredFeedback rejects; omitted score item rejects even when total matches; valid explicit zero for every item accepts.
- [x] Cover duplicate/unknown IDs, fractional points, checks consistent with full-credit items, total mismatch, missing evidence/review reason, unknown anchors and stale snapshots. Preserve manual checklist scoring behavior.
- [x] Run focused tests before changes; record expected failing assertions.
- [x] Tighten validation once in shared proposal ingress. Do not globally tighten `GradingJson.score`, which also serves legacy/manual grading.
- [x] Verify both API callback and single/batch MCP rejection leave no READY proposal. Retest exact retry, conflict, legacy history and teacher review guards.
- [x] Run `mvn '-Dtest=GradingProposalValidatorTest,GradingProposalServiceTest,GradingProposalResultHandlerTest,GradingMcpReplayTest,GradingMcpToolsTest,GradingPromptSchemaTest' test` in `backend/`; zero failures/errors, account for any skips.

Proposed commit: `fix(grading): require complete evidence-backed AI proposals`.

### Task 2: Align MCP grading context with the API — R03

**Modify:** `GradingPromptBuilder.java`, `GradingMcpQueryService.java`, `GradingProposalTransactions.java` in the grading service package. Reuse `IAssignmentQuery.getContent(UUID)` and `AssignmentContent`; do not access assignment tables directly.

**Tests:** `GradingMcpQueryServiceTest.java`, `GradingPromptSchemaTest.java`, `GradingMcpBatchServiceTest.java`.

**Interfaces:** Extract `GradingPromptBuilder.gradingInstructions(): String` from the existing system policy and use it for API/MCP. Add an `assignmentContext` object containing `assignmentId`, `versionNo`, `instructions`, `skeleton`, `requiredDiagrams` to MCP context and include it in the fingerprint. `rubricSnapshot`, `outputSchema`, `submissionRef`, `submissionHash`, `expectedVersion`, `complete` and `continuation` keep existing names.

- [x] Test API/MCP use identical grading policy, context includes the actual question text, and payload added here has no names/emails/member contribution scores.
- [x] Test changed instructions/skeleton/version during continuation rejects; recheck locked rubric consistency using the existing API rule.
- [x] Build the common context after existing scope/current-submission guards. Preserve untrusted-content labeling, alternative-solution/review instructions and group-root-only grading.
- [x] Include new fields in byte budgeting. If fixed context or a single block is too large, return an explicit bounded error; never silently truncate. Test multibyte Vietnamese near the limit.
- [x] Run `mvn '-Dtest=GradingMcpQueryServiceTest,GradingPromptSchemaTest,GradingMcpBatchServiceTest,GradingProposalServiceTest' test` in `backend/`.

Proposed commit: `fix(mcp): share complete grading context and policy`.

### Task 3: Repair per-client setup and current documentation — R04/R05

**Modify:**
- `frontend/src/features/ai/components/SetupGuides.tsx`
- `frontend/src/features/ai/components/McpAgentSection.tsx`
- `frontend/src/features/ai/components/McpAgentsTab.tsx` if guide placement needs adjustment.
- `docs/guides/mcp-clients.md`
- `aidlc-docs/inception/requirements/2026-10-26-mcp-staff-expansion.md` (supersession notice only).
- Related U06/U09 current docs where they contradict the approved current policy; preserve dated evidence.

**Tests:** Create `frontend/tests/features/ai/components/SetupGuides.test.tsx`; adjust `frontend/tests/features/ai/pages/AiConnectionPage.test.tsx`. Reuse `scripts/check-mcp.ps1` and `scripts/test-check-mcp.py`.

**Interfaces:** Add a client discriminator for config generation. Codex gets TOML/environment instructions, OpenCode gets `mcp`/`type:remote`/`oauth:false`, Gemini CLI gets `mcpServers`/`httpUrl`. Validate installed Antigravity config/help before supplying an adapter; label it unverified if it cannot be checked. Keep setup preview and token-dialog copy behavior consistent; tokens remain masked outside explicit reveal.

- [x] Check all callers of `mcpConfigSample` before changing its signature. Test each client format, endpoint interpolation, placeholder preview and explicit-token copy separately.
- [x] Give numbered steps: create the correct access token -> edit the named config location -> provide token in the launching process -> restart -> diagnostic -> `get_my_capabilities` -> one authorized read.
- [x] Explain read-only vs read-write, local/remote/container addresses, proxy/direct endpoint, Origin rejection, API key vs MCP token and no provider call by MCP.
- [x] Reconcile one-time/reveal wording with `revealable`; mark historical policies as superseded and distinguish handshake/read/write verification.
- [x] Run `npm test -- tests/features/ai/components/SetupGuides.test.tsx tests/features/ai/pages/AiConnectionPage.test.tsx` in `frontend/`; Python fixture pending: no Python runtime installed; no runtime was installed as part of this task.
- [x] During implementation, check official client docs and record tested installed versions. Test authenticated clients only using credentials explicitly provided/authorized for that check; never invent a success record.

References previously reviewed: https://opencode.ai/docs/mcp-servers/ and https://geminicli.com/docs/tools/mcp-server/ . Recheck at implementation time.

Proposed commit: `fix(mcp): provide client-specific connection guides`.

### Task 4: Remove full-document reads from discovery — R06

**Modify:** `GradingMcpQueryService.java`, `SubmissionReviewService.java`; reuse `IEvaluationRepository.findForAssignmentAndClass(UUID, UUID)`. Touch proposal repository/contracts only if a measured status-filter query needs batching; do not introduce a broad repository framework.

**Tests:** Extend `GradingMcpQueryServiceTest.java` and create `SubmissionReviewMcpListingTest.java` under the grading service test package.

**Interfaces:** Extract/reuse the existing authorized metadata-list construction within SubmissionReviewService so UI and MCP share scope/current-root rules. Build once per request; apply state/late/type/proposalStatus before MCP's zero-based pagination. Preserve `list_submissions` result fields and totals; group MEMBER evaluations are not grading roots. No document/content/activity call is needed to construct a row.

- [x] Build a 200-submission fixture, request size 20 and verify exact filtered page/total, no `reviews.detail`, no per-row `getSubmission`, and no `activity.summary` calls.
- [x] Cover individual/group/subject-wide assignments, empty/not-submitted rows, removed access and invalid filters. Verify list pages share the same ordering with no duplicates/omissions.
- [x] Replace detail-dependent enrichment with authorized evaluation metadata; avoid rebuilding the full roster for each internal page.
- [x] Run `mvn '-Dtest=GradingMcpQueryServiceTest,SubmissionReviewMcpListingTest,GradingAccessTest' test` in `backend/`.
- [ ] Measure SQL/service calls and latency on representative local data; report actual numbers and environment. Unit call counts alone do not prove a latency target.

Proposed commit: `perf(mcp): list submission metadata without document reads`.

### Task 5: Expose AI rubric limits during authoring — R07

**Modify:** `AssignmentDraftValidator.java`, `McpAuthoringRequirementsTools.java`, assignment AI-draft/authoring response mapping as needed, `frontend/src/features/assignments/components/AiDraftPreview.tsx` and `RubricEditor.tsx`.

**Tests:** `AssignmentMcpAuthoringServiceTest.java`, `AssignmentMcpToolsTest.java`, authoring validator tests; `frontend/tests/features/assignments/components/AiDraftPreview.test.tsx` plus the existing rubric/editor suite found by caller search.

**Interfaces:** Expose `maxAiGradingRubricItems=30` from the existing server constant in authoring requirements. Add warning `AI_GRADING_RUBRIC_LIMIT` to draft validation when total item count exceeds the limit. For manual editor warnings, pass server-provided limit through existing authoring metadata; do not duplicate an independent magic number in TypeScript. Do not change `targetPoints` to a default 10.

- [x] Cover 30 and 31 items, nested leaf sections, non-10-point scale and Practice vs Graded behavior. Warning does not make otherwise valid manual work invalid.
- [x] Show a brief persistent warning beside rubric/draft validation explaining manual grading remains available. Keep backend AI rejection explicit for an oversized request.
- [x] Verify warnings appear both for agent-created proposals and manual rubric edits before publication; retain all items and their points.
- [x] Run affected authoring backend/frontend tests and `npm run typecheck`.

Proposed commit: `fix(assignments): surface AI grading rubric limits`.

### Task 6: Package two skills and optional hook adapters — R08/R09

**Create proposed repository-local files:**
- `agent-skills/author-assignment/SKILL.md`
- `agent-skills/grade-with-rubric/SKILL.md`
- `docs/guides/mcp-agent-workflows.md`
- `scripts/mcp-hooks/validate-proposal.mjs`, `scripts/mcp-hooks/summarize-result.mjs` and `scripts/mcp-hooks/test_hooks.mjs` only for client adapters whose event formats are verified.

**Interfaces:** Skills consume current capabilities, authoring requirements, grading context and tool result schema, not copied static UUIDs. Hook adapters consume the documented JSON stdin/stdout contract for their client; use Node standard library (Python runtime unavailable). Match only this platform's proposal tools. Never assume an unsupported hook format works in another client. Read skill-creator/writing-skills instructions when actually authoring these packages.

- [x] Authoring skill: identify target -> read authorized materials with continuation -> fetch requirements -> draft questions and matching rubric -> self-check requested scale/coverage -> create with requestRef -> return reviewUrl. No generation rerun just to open review.
- [x] Grading skill: select pending/draft roots -> read full context -> assess each item -> cite observable evidence or explicit review reason -> save -> report per-item result. Use one student submission per reasoning context where supported; no contribution scoring or extra criteria.
- [x] Bound retries to two retries per failed operation. Reuse requestRef only for unchanged payload; stale context means reread/regrade and a new requestRef. Respect rate-limit errors and byte limits; split batches on bytes as well as count.
- [x] BeforeTool checks envelope/required fields locally when context is available; it does not claim to verify reading completion or semantic truth. Backend performs all authoritative checks.
- [x] AfterTool emits only status, proposalId/index and safe error code. It must not run its own retry, mutate data or call another model; the skill handles the next action.
- [x] Test malformed stdin, unknown tool, missing context, missing feedback, batch partial error, retry identity and token redaction with dummy data. Run `node scripts/mcp-hooks/test_hooks.mjs` if these scripts are created.
- [x] Document how to install/disable per client without changing global configurations in this task. If no suitable client hook is available, ship the skills/backend protections and label the adapter unavailable.

Proposed commit: `feat(mcp): add authoring and grading agent workflows`.

### Task 7: Quality pilot, integration evidence and documentation closure — R10

**Create:** `docs/guides/mcp-grading-quality.md`, `backend/src/test/resources/grading-quality/README.md`, `aidlc-docs/construction/build-and-test/2026-11-01-mcp-quality-remediation-results.md` when execution evidence exists. Store actual fixtures only after de-identification and permission; do not fabricate teacher reference grades.

- [x] Define the proposed 20-case pilot: complete, incomplete/empty, partial-credit, equivalent alternative solutions, diagrams/images that cannot be read, contradictory evidence and embedded instructions to manipulate grading.
- [x] Record assignment/rubric/context versions, model identifier, policy version, run date, expected/actual score per item and review flags. Separate deterministic validator tests from model-quality measurements.
- [ ] Obtain teacher reference scores and agreement on acceptable per-item/total error. Until then label semantic quality pending, not passed. Report unsupported evidence and missed mandatory requirements separately from average score error.
- [x] Run backend focused suites from Tasks 1–5, then `mvn test`; inspect failures/skips and the repository's required-DB-suite guard when its prerequisites are available.
- [x] Run frontend `npm test`, `npm run lint`, `npm run typecheck`, `npm run build` once after integrated source changes. Record existing warnings separately.
- [ ] Use Orca to review connection guide/client switching and authoring warnings. With authorized test credentials, verify discovery -> draft review -> complete grading context -> proposal -> teacher review. Official grade publication is unnecessary for this acceptance.
- [x] Run `git diff --check`, record source revision and exact client/runtime limitations; update AI-DLC state/audit/current guide without changing historical test claims. Keep provider calls and credential installation pending if authorization/data are absent.

Proposed commit: `docs(mcp): document validation and grading quality evidence`.

## Self-review and handoff

Coverage: R01/R02 -> Task 1; R03 -> Task 2; R04/R05 -> Task 3; R06 -> Task 4; R07 -> Task 5; R08/R09 -> Task 6; R10 -> Task 7. All seven audit findings have a repair or explicit measurement gate. Critical backend checks do not depend on an optional hook.

Recommended execution: native, sequential implementation with a review after Tasks 1–2 before client packaging. This is a recommendation, not a selected execution mode. Review this plan and its proposed requirements before implementing. Existing AI-DLC security/resiliency scope remains enabled; PBT disabled. No deployment, global installation or data export is included.

## Execution rulings and remaining gates

### Follow-up: copy agent setup prompt

User authorized: “thêm nút prompt copy để đưa vào agent tự setup luôn”. Scope: existing McpClientSetup shared by guide and token dialog; no new backend or global installer. Reuse selected client/native sample and house Button. Preserve existing servers, require private backup and safe read verification; placeholder mode must never claim real authentication. Explicit token-dialog copying discloses that the prompt contains the token. Provide preview/manual copy and clipboard failure feedback.

- [x] Add regression tests for selected-client prompt, placeholder vs explicit token, and denied clipboard access; observe missing-button failures before implementation.
- [x] Implement copy/preview in shared setup component and update the guide.
- [x] Complete focused tests, typecheck, lint, production build and rendered browser review; record results in the existing results artifact. Follow-up steering: prompt content is English, interface labels remain Vietnamese.

- User requested Codex, Claude Code, Antigravity and OpenCode, then explicitly selected setup without terminal commands. UI names drop CLI; personal config samples use direct Bearer headers. Claude Code uses ~/.claude.json rather than a tracked project .mcp.json. No real token was generated or written by this task.
- Existing 2026-10-26 requirements already had a supersession notice; preserve it. The preceding audit overstated this issue.
- Server exposes one AiGradingLimits contract constant and the assignment detail DTO exposes the limit to the live editor. Warning UI is implemented at AssignmentFormPage, reusing the existing preview warning rendering.
- Node hook adapters are verified only against Gemini's documented event format, not all clients. Both skills remain repository-local. Pressure tests found safe baseline behavior; no measured behavior gain claimed.
- Full backend runtime suite passed 1,630 cases; the combined command then exhausted Metaspace entering ArchUnit. Architecture was rerun separately and passed 2 cases. Do not describe the original combined command as successful.
- Frontend full suite passed 644 cases before final setup refinements; affected setup suites passed again after the final changes. Production build, typecheck and lint rerun after the final source edits.
- Orca guide/client switching and mobile overflow checked; authenticated authoring/grading writes and complete teacher-review flow not executed. External-client credential acceptance, representative SQL/latency measurement, Python diagnostic fixtures and actual teacher-scored model quality remain pending.
