# U09 AI Result Delivery Implementation Plan

> For Gemini 4 Argon in the existing Orca terminal: implement sequentially in this checkout. Read AGENTS.md and CLAUDE.md, preserve current changes, and update these checkboxes. The user authorized investigation, this plan, and implementation handoff. Do not create another agent or checkout.

**Goal:** Deliver completed AI grading feedback promptly and show accurate progress instead of leaving a completed request looking stuck.

**Architecture:** Keep provider execution separate from transactional result delivery. Preserve idempotency, grading snapshot validation, authorization, and quota accounting. Diagnose the first-delivery failure before choosing a repair; keep the scheduled scanner as recovery rather than the normal completion path.

**Tech stack:** Java/Spring Boot/PostgreSQL/RabbitMQ; Next.js/React/TypeScript/Vitest.

## Verified incident and uncertainty

- Class: `4d18e255-1789-45d6-8fbb-351e2188f672` (U05-A).
- Evaluation: `8b88dd01-6d2f-48df-9bd0-6525e6627800`.
- Proposal: `505a1202-e78c-42f6-ae40-5d921bab28bf`.
- AI task: `819b7c93-746d-4c06-9e67-af840ed9a1fd`.
- 2026-10-26, Asia/Saigon: submitted 10:02:19.950; provider/task READY 10:02:23.001; proposal completed 10:03:50.079. Delivery lag approximately 87 seconds.
- During investigation task was READY with handler_done=false while proposal stayed PENDING. Subsequently handler_done=true, apply_result=APPLIED, proposal=READY.
- No new NoClassDefFoundError was observed for this incident. An earlier, separate runtime incident was resolved by clean build/restart after class files changed under the running JVM. Do not conflate these incidents.
- AiTaskJobHandler.handle calls deliverOrRetry after persisting finish. scan retries unhandled terminal tasks older than one minute. ScheduledScanRunner scans every 60 seconds. quietDeliver suppresses RuntimeException without logging. This is consistent with delayed recovery; it does NOT prove why immediate delivery failed.
- Existing logs contain disconnected SSE IOException messages. Causation between those errors and delivery delay has not been established.
- Frontend useGradingProposals polls every 3 seconds, stops on a request exception, and silently stops after 100 pending polls. AiProposalPanel describes every PENDING proposal as AI still reading.

## Constraints and acceptance

- Work directly in `F:/code/git/AI-Powered-Learning-Platform`, branch mcp-test; inspect git status/diff again before editing.
- No commit, push, merge, deploy, key/model changes, security weakening, or new connections.
- Do not finalize/publish grades, apply AI scores, overwrite teacher input, or delete existing test records. Use dedicated disposable fixtures for tests.
- Never log keys, authorization headers, prompts, submissions, provider output, or raw unsanitized exception bodies. Log correlation IDs, stages, duration and safe error classification.
- Provider returns once; duplicate delivery/retry must not call provider again or add another usage/quota charge.
- Normal result delivery must finish in the same job flow and appear by the next ordinary UI polling cycle after successful delivery, with no dependence on the one-minute scanner. Demonstrate this in deterministic tests rather than claiming a universal real-network SLA.
- Recovery from transient handler failure must be observable and bounded. Preserve terminal errors, rejection/stale behavior, and valid manual grading throughout.
- Do not mark the bug fixed based only on automated tests. Report live UI validation separately; if left to the user, mark it pending and provide the manual guide below.

## Task 1: Reproduce and identify the failing boundary

Primary files:
- `backend/src/main/java/edu/aiplatform/ai/application/AiTaskJobHandler.java`
- `backend/src/main/java/edu/aiplatform/ai/application/AiTaskDelivery.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalTransactions.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java`
- `backend/src/main/java/edu/aiplatform/jobs/application/JobDispatcher.java`
- `backend/src/main/java/edu/aiplatform/jobs/application/ScheduledScanRunner.java`

- [x] Read existing AiTaskFlowTest, GradingProposalServiceTest, GradingProposalResultHandlerTest, repository adapters, transaction propagation, and dispatcher retry behavior.
- [x] Add minimal safe stage logging for provider-finished, delivery-attempt/success/failure, scanner recovery, task/proposal IDs and elapsed times. Replace silent recovery failure with safe observable diagnostics; do not dump sensitive exception content.
- [x] Reproduce with a fake fast provider and the real grading callback/persistence boundary where possible. Exercise callback-before-task-ID-attachment and concurrent proposal polling.
- [x] Record the actual failure cause and reproduction in the implementation report. If it cannot be reproduced, explicitly state the uncertainty; do not invent a lock/race explanation or change provider settings.
- [x] Add a failing regression test for the proven cause. Mock-only flow tests are insufficient for a proven transaction/JPA defect; use PostgreSQL integration coverage for that boundary.

## Task 2: Repair delivery using the proven cause

- [x] Make the smallest repair at the failing boundary. Preserve task completion commit before callback execution, task/proposal correlation, requester authorization, and submission/rubric version checks.
- [x] Keep delivery idempotent under concurrent worker/scanner/polling activity; establish a consistent lock order if locking is implicated.
- [x] Ensure a transient handler failure uses the existing bounded delivery retry path without repeating provider execution. Keep the minute scanner as fallback; simply reducing its interval is not a root-cause fix.
- [x] Verify READY persists only for a validated proposal. Permanent invalid result, stale snapshot and revoked access must resolve clearly rather than remain PENDING indefinitely.
- [x] Avoid external provider calls or blind task replay from GET polling. If response reconciliation is needed, use an authorized unit contract and preserve read/write transaction boundaries.
- [x] Run focused tests: AiTaskFlowTest, GradingProposalServiceTest, GradingProposalResultHandlerTest, GradingProposalValidatorTest, JobDispatcherTest, plus the new regression/integration test. Resolve the installed Maven command using scripts/run-backend.bat; no backend Maven wrapper was found during planning.

Required cases: fast normal success without scanner; delayed task-ID attachment; handler fails once then succeeds; duplicate delivery; polling while callback updates; stale rubric/submission; invalid AI output; callback authorization failure; provider exception; exactly one provider call and usage record across delivery retries.

## Task 3: Accurate UI progress and resilient polling

Files:
- `frontend/src/features/grading/useGradingProposals.ts`
- `frontend/src/features/grading/AiProposalPanel.tsx`
- `frontend/src/features/grading/api.ts`
- `frontend/src/features/grading/types.ts`
- Existing grading component tests; add focused hook tests if absent.

- [x] While proposal=PENDING, use the authorized task metadata to distinguish QUEUED, RUNNING and task READY awaiting proposal validation/delivery. Prefer additive optional metadata or the existing task endpoint; do not fabricate stages from elapsed time alone.
- [x] Use clear Vietnamese copy: queued = "Đang chờ xử lý"; running = "AI đang chấm bài"; task READY/proposal PENDING = "Đã nhận kết quả AI, đang xử lý phản hồi". Enable Apply only when proposal is validated READY.
- [x] Retry transient polling/network errors with bounded backoff and avoid repetitive error notifications. Handle 401/403/404 appropriately, cancel stale requests/timers on navigation, and prevent overlapping polls.
- [x] On polling exhaustion, show an explicit delayed-status message and "Kiểm tra lại" action that checks the existing request. Do not automatically submit another AI job. Preserve manual rubric edits.
- [x] Add deterministic fake-timer tests for phases, recovery after temporary network failure, polling limit/manual resume, navigation cleanup, and unchanged teacher scores. Ensure tests pin behavior rather than implementation details.
- [x] Run focused grading tests, `npm --prefix frontend run lint`, and `npm --prefix frontend run typecheck`. Run an isolated production build if API/type/component changes justify it, without replacing the live frontend's .next directory.

## Task 4: Runtime check and handover

- [x] Identify the existing host API/worker and frontend before restarting; never start another Docker worker. Existing Orca tabs are U09 Backend API worker and U09 Frontend, currently on ports 8080/3000.
- [x] Before Maven clean/build/tests, stop the owned backend or otherwise isolate build output so the running JVM cannot lose its class files. Check whether Java IDE autobuild also writes backend/target/classes. Do not kill unrelated Java processes or silently change global IDE settings.
- [ ] Run one live AI proposal against a dedicated submission using the existing personal Gemini connection. Record task READY time, delivery time, proposal READY time and UI transition. Do not apply/finalize/publish scores.
- [x] Run `git diff --check` and inspect final diff for secrets or unrelated changes.
- [x] Write `aidlc-docs/construction/build-and-test/2026-10-26-u09-ai-result-delivery-report.md`: proven cause vs unknowns, files changed, exact checks/counts, observed timings, UI evidence or explicit pending status, risks and manual test results.
- [x] Update this plan, aidlc-state.md and append audit.md. Finish with a concise Vietnamese report. Stop; do not commit or push.

## User manual test guide (record Pass / Fail / Pending)

1. Open a dedicated submitted evaluation while logged in as teacher. Note its ID and existing saved grade. Keep the current real submission unchanged.
2. Enter 0.5 in a rubric item capped at 1 without saving; request AI using the existing personal Gemini connection. Verify the input remains 0.5 through every state.
3. Observe queued/running/result-processing/READY states. Record task ID and provider-finished/proposal-completed timestamps; a fast success should not consistently wait for the minute scanner.
4. Verify score summary and feedback appear; Apply stays disabled until the proposal is READY. No draft score is saved or published merely by receiving a result.
5. Briefly interrupt network connectivity during polling, restore it, and verify the same request recovers without another provider call. Confirm auth/permission failures remain clear.
6. Exercise polling exhaustion using a test fixture/fake timer rather than waiting five minutes; "Kiểm tra lại" resumes checking the existing job.
7. Use backend integration fixtures to simulate one callback failure, duplicate delivery and a stale submission. Verify recovery/rejection, one provider call and one usage charge.
8. Navigate away/back or reload; existing proposal is visible, no duplicate request is sent, and unsaved-input behavior follows the existing product flow.

## Additional feedback-design request

The user also asked Codex to review `C:\Users\Quang Hung\Downloads\Bao_cao_7A1.pdf` and recommend clearer grading feedback. This is a separate design recommendation, not authorization to expand this bugfix. Do not transmit that PDF or implement new feedback schemas/UI until its analysis is available and the scope is agreed.
