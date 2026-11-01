# External-agent workflow implementation plan

> Execute inline using superpowers:executing-plans. User explicitly authorized both planning and implementation; proceed without a second approval checkpoint. No automatic commits.

**Goal:** Start in external agent chat, clarify intent there, enforce complete grading reads and return exact UI review links.

**Architecture:** Reuse current authoring/grading MCP tools and review screens. Store ephemeral contiguous read progress in existing Redis; UI handoff uses proposalId, not a new workflow session.

**Tech stack:** Java/Spring, Redis, React/TypeScript, JUnit/Mockito and Vitest; no new dependencies or migration.

**Spec:** [Requirements and stories](../../../inception/requirements/2026-11-01-external-agent-workflow.md).

## Review focus

Skipped/expired pages; changed assignment policy with unchanged submission hash; actor isolation; batch responses discarded by byte budget; older/stale proposal links that must not apply scores. Preserve existing manual/API behavior and role filtering.

### Task 1 — server-controlled complete reads

Files: grading/service/GradingMcpReadProgress.java (new), GradingMcpQueryService.java, GradingMcpTools.java, GradingMcpBatchService.java, associated tests and catalog constructor fixtures.

- [x] Add regressions and observe RED for missing fingerprint/progress, page gaps and batch budget drops.
- [x] Implement atomic Redis contiguous offset tracking, 30-minute inactivity TTL, fail-closed errors and empty-document support. Snapshot fingerprint must be rechecked at proposal ingress; output pages record progress only after they fit the response budget.
- [x] Extend tool schema with required snapshotFingerprint. Preserve current authorization/hash/version/rubric guards and transaction replay behavior. Re-read after progress expiry rather than forging success.
- [x] Run focused tests, Redis script check and architecture rules.

### Task 2 — exact review handoff

Files: AssignmentMcpTools.java, GradingMcpTools.java, GradingMcpQueryService.java; frontend GradingWorkspacePage.tsx, AiProposalPanel.tsx and tests.

- [x] Test and add reviewUrl to revised assignment and single/batch/listed grading results.
- [x] Open the requested grading proposal via proposalId without applying it; expand older proposals and report an unavailable ID visibly. Preserve existing student navigation and manual flow.
- [x] Run affected frontend/backend tests and typecheck/build.

### Task 3 — agent conversation and AI-DLC closure

Files: McpCapabilitiesTools.java, McpAuthoringRequirementsTools.java; agent-skills/*/SKILL.md; docs/guides/mcp-agent-workflows.md, mcp-clients.md; current U06/U07/U09 business rules; AI-DLC state/audit/results.

- [x] Expose concise external-agent/clarification/review guidance through current read tools. Keep Student capabilities separate from educator workflows.
- [x] Strengthen both skills: infer available facts, group material questions, clarify ambiguous targets before writes, return actual review URLs and distinguish unsupported evidence from certainty.
- [x] Pressure-check ambiguous requests and new read/proposal contract, review final diff, record test evidence and remaining real-model/client acceptance limits.

Scope exclusions: no global skill installation, ZIP installer, client-specific hook port, automatic publishing, new UI chat or provider/model calls. Existing security/resiliency enabled; PBT remains disabled. Work stays in current Orca checkout to preserve pending authorized changes and running app.

## Completion evidence

Implemented and verified. [Results and remaining acceptance](../../build-and-test/2026-11-01-external-agent-workflow-results.md). Fresh review corrected escaped-envelope progress and restored batch transaction isolation. Redis Lua checked against local Redis; old proposal link visually reviewed through Orca without writes. Backend 67 focused/architecture tests and frontend 38 affected tests passed. No commit/push, new model call or global install. Full teacher-scored model calibration and authenticated four-client acceptance remain pending.
