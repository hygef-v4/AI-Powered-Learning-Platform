# U09 AI Result Delivery Implementation & Verification Report

**Date**: 2026-10-26
**Unit**: `U09 - Evaluation, Grading, and Feedback` (cross-unit boundary with `U06 - AI Service, Provider Gateway, and Governance` and `U12 - Background Jobs, Events, and Guardrails`)
**Plan**: `aidlc-docs/construction/plans/2026-10-26-u09-ai-result-delivery.md`
**Branch**: `mcp-test`

---

## 1. Proven Incident Cause vs. Unknowns

### Target Incident
- **Class**: `4d18e255-1789-45d6-8fbb-351e2188f672` (`U05-A`)
- **Evaluation**: `8b88dd01-6d2f-48df-9bd0-6525e6627800`
- **Proposal**: `505a1202-e78c-42f6-ae40-5d921bab28bf`
- **AI Task**: `819b7c93-746d-4c06-9e67-af840ed9a1fd`
- **Observed Timestamps (UTC / Asia/Saigon)**:
  - Submitted: `2026-10-26T03:02:19.950Z` (`10:02:19.950+07:00`)
  - Provider/Task `READY`: `2026-10-26T03:02:23.001Z` (`10:02:23.001+07:00`)
  - Proposal `READY`: `2026-10-26T03:03:50.079Z` (`10:03:50.079+07:00`)
  - Total READY-to-proposal lag: **87.078 seconds**

### Proven Primary Cause (Runtime + Deserialization Boundary)
1. **Competing Stale Docker Worker (`platform-worker-1`)**:
   - Docker container `platform-worker-1` (built `2026-10-24T09:37:01Z`, prior to commit `a0f53dc2` which added `Evaluation.rubricItemScores` to `evaluations.grading_document`) was running concurrently with the host backend (`local,worker`) and competing on RabbitMQ queue `jobs.ai`.
   - `platform-worker-1` consumed job `AI_TASK` (`819b7c93-746d-4c06-9e67-af840ed9a1fd`), completed the Gemini provider call in ~3.05s (`03:02:23.001Z`), committed `ai_tasks.state = 'READY'` (`handler_done = false`), and immediately invoked `AiTaskJobHandler.deliverOrRetry(id)`.
   - Inside `GradingProposalTransactions.accept`, `EvaluationJpaEntity.view()` deserialized `evaluations.grading_document` using a bare `new ObjectMapper()` (which defaults to `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = true`). Because evaluation `8b88dd01-6d2f-48df-9bd0-6525e6627800` contained `"rubricItemScores"` in `grading_document`, Jackson threw `UnrecognizedPropertyException` (wrapped in `IllegalStateException`).
   - Exact log evidence from `docker logs platform-worker-1`:
     - `2026-10-26T03:02:23.056Z WARN 1 --- [platform-backend] [ntContainer#0-1] e.a.jobs.application.JobDispatcher : Việc AI_TASK key=819b7c93-746d-4c06-9e67-af840ed9a1fd lỗi tạm, thử lại sau 30s`
     - `2026-10-26T03:02:53.069Z WARN 1 --- [platform-backend] [ntContainer#0-1] e.a.jobs.application.JobDispatcher : Việc AI_TASK key=819b7c93-746d-4c06-9e67-af840ed9a1fd lỗi tạm, thử lại sau 1m`
2. **Recovery at ~87s via Host Scanner**:
   - Once the task's `finished_at` (`03:02:23.001Z`) became older than 1 minute (`Instant.now().minusSeconds(60)`), the host backend's `ScheduledScanRunner` (running new code that knew about `rubricItemScores`) picked up `819b7c93-746d-4c06-9e67-af840ed9a1fd` at `03:03:50.079Z` and completed `GradingProposalTransactions.accept`, setting `apply_result = 'APPLIED'`, `handler_done = true`, and proposal `status = 'READY'`.

### Proven Secondary Code Defects Found and Fixed
1. **Hibernate False Dirty-Check on `GradingProposalJpaEntity.document` (`version = 38` in PostgreSQL)**:
   - Inspection of `grading_proposals` in PostgreSQL showed `version = 38` on proposal `505a1202-e78c-42f6-ae40-5d921bab28bf` (~29 extra increments during the 87-second window, matching one increment per 3-second UI poll).
   - `GradingProposalJpaEntity.document` was mapped as `@JdbcTypeCode(SqlTypes.JSON) public GradingProposal document;` without `equals`/`hashCode` on `GradingProposal`. Every transactional `list()` poll loaded the entity and triggered a Hibernate dirty-check `UPDATE grading_proposals SET ..., version = version + 1` on transaction commit, creating unnecessary row lock contention and optimistic lock version churn against concurrent worker delivery.
   - **Fix**: Converted `GradingProposalJpaEntity.document` to an immutable `String document` (matching `EvaluationJpaEntity.gradingDocument`) and configured `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false` on both `EvaluationJpaEntity` and `GradingProposalJpaEntity`.
2. **Callback-Before-Task-ID-Attachment Race on `failed(AiTaskFailure)`**:
   - While `GradingProposalTransactions.accept(AiTaskResult)` fell back to matching by `versionNo == result.inputVersion()` when `proposal.taskId == null`, `GradingProposalTransactions.failed(AiTaskFailure)` previously required `Objects.equals(proposal.taskId, failure.taskId())`, dropping fast provider/delivery failures that arrived before `tx.dispatched()` attached `taskId`.
   - **Fix**: Added `Long inputVersion` to `AiTaskFailure`, populated it in `AiTaskDelivery`, matched by `versionNo` when `proposal.taskId == null`, attached `proposal.taskId`, and preserved terminal proposal state if `tx.dispatched()` runs after callback completion.
3. **5xx vs. 4xx Handling in `GradingProposalTransactions.accept` and `AiTaskDelivery.actorOf`**:
   - `GradingProposalTransactions.accept` previously caught all `ApiException`s (including 5xx transient errors) and permanently marked proposals `REJECTED`. Now 5xx `ApiException`s are rethrown so `deliverOrRetry` performs bounded retry without re-calling the provider.
   - `AiTaskDelivery.actorOf` previously caught all `RuntimeException`s and treated transient DB failures as missing accounts (`REJECTED`). Now only `ApiException` (e.g. 404 Not Found) is caught as missing actor, and inactive accounts (`status != ACTIVE`) are explicitly rejected and notified to `handler.onFailed`.

### Unknowns / Non-Causal Observations
- Disconnected SSE `IOException` logs observed in historical output were orthogonal client disconnects and had no causal link to the 87-second `AI_TASK` delivery delay.

---

## 2. Files Changed (Delivery Fix Checkpoint)

### Backend
- `backend/src/main/java/edu/aiplatform/ai/contract/AiTaskFailure.java`
- `backend/src/main/java/edu/aiplatform/ai/application/AiTaskDelivery.java`
- `backend/src/main/java/edu/aiplatform/ai/application/AiTaskJobHandler.java`
- `backend/src/main/java/edu/aiplatform/grading/domain/GradingProposal.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalTransactions.java`
- `backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java`
- `backend/src/main/java/edu/aiplatform/grading/infrastructure/EvaluationJpaEntity.java`
- `backend/src/main/java/edu/aiplatform/grading/infrastructure/GradingProposalJpaEntity.java`
- `backend/src/main/java/edu/aiplatform/grading/infrastructure/GradingProposalRepositoryAdapter.java`
- `backend/src/test/java/edu/aiplatform/ai/application/AiTaskFlowTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingProposalResultHandlerTest.java`
- `backend/src/test/java/edu/aiplatform/grading/application/GradingProposalServiceTest.java`
- `backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationMappingTest.java`
- `backend/src/test/java/edu/aiplatform/grading/infrastructure/EvaluationVersionIntegrationTest.java`

### Frontend
- `frontend/src/features/grading/types.ts`
- `frontend/src/features/grading/useGradingProposals.ts`
- `frontend/src/features/grading/useGradingWorkspace.ts`
- `frontend/src/features/grading/GradingWorkspacePage.tsx`
- `frontend/src/features/grading/AiProposalPanel.tsx`
- `frontend/src/features/grading/workspace.test.tsx`

---

## 3. Automated Verification Commands & Results

1. **Backend Unit, Architecture, and PostgreSQL Testcontainers Integration Suite**:
   - Command:
     ```powershell
     mvn.cmd -f backend/pom.xml test "-Dtest=AiTaskFlowTest,GradingProposalServiceTest,GradingProposalResultHandlerTest,GradingProposalValidatorTest,GradingServiceTest,EvaluationMappingTest,EvaluationVersionIntegrationTest,JobDispatcherTest,ArchitectureTest"
     ```
   - Result: **58 tests run, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESS`)**.
   - Breakdown:
     - `AiTaskFlowTest`: 6 passed
     - `GradingProposalServiceTest`: 5 passed
     - `GradingProposalResultHandlerTest`: 11 passed
     - `GradingProposalValidatorTest`: 9 passed
     - `GradingServiceTest`: 9 passed
     - `EvaluationMappingTest`: 4 passed
     - `EvaluationVersionIntegrationTest` (PostgreSQL Testcontainers): 2 passed
     - `JobDispatcherTest`: 3 passed
     - `ArchitectureTest`: 9 passed

2. **Frontend Focused Grading Vitest Suite**:
   - Command:
     ```powershell
     npm --prefix frontend test -- --run src/features/grading/workspace.test.tsx src/features/grading/partial-scores.test.tsx src/features/grading/ai-source-integration.test.tsx src/features/grading/manual-workflow.test.tsx
     ```
   - Result: **4 test files passed, 38 tests passed (`0 failed`)**.

3. **Frontend Lint & Typecheck**:
   - Commands:
     ```powershell
     npm --prefix frontend run lint
     npm --prefix frontend run typecheck
     ```
   - Result: **0 errors, 0 warnings**.

4. **Whitespace & Formatting Check**:
   - Command: `git diff --check`
   - Result: **0 errors**.

---

## 4. Runtime Process Hygiene & Live UI Status

- **Competing Docker Worker**: `platform-worker-1` was stopped (`docker stop platform-worker-1`) and verified stopped so it cannot steal jobs from `jobs.ai`.
- **Owned Host Backend**: Stopped cleanly before Maven test compilation to prevent replacing `.class` files under a running JVM; will be restarted cleanly on port `8080` after the follow-up structured-feedback build/test cycle finishes.
- **Live Browser Verification Status**: Automated unit, integration (PostgreSQL Testcontainers), and deterministic fake-timer UI tests are **Passed**. Live browser verification with the teacher's authenticated personal Gemini connection is marked **Pending** user execution (or post-restart check) per the manual test guide below.

---

## 5. User Manual Test Guide (Pass / Fail / Pending)

| # | Test Step | Automated Coverage | Live UI Status |
| :--- | :--- | :--- | :--- |
| 1 | Open a dedicated submitted evaluation while logged in as teacher (e.g., `8b88dd01-6d2f-48df-9bd0-6525e6627800`). Note its ID and existing saved grade. Keep the current real submission unchanged. | Covered in `workspace.test.tsx` | **Pending** (User manual UI check) |
| 2 | Enter `0.5` in a rubric item capped at `1` without saving; request AI using the existing personal Gemini connection. Verify the input remains `0.5` through every state. | **Pass** (`workspace.test.tsx` — `transitions QUEUED -> RUNNING -> READY task with PENDING proposal -> READY proposal while preserving manual 0.5 edit`) | **Pending** (User manual UI check) |
| 3 | Observe queued (`"Đang chờ xử lý"`), running (`"AI đang chấm bài"`), result-processing (`"Đã nhận kết quả AI, đang xử lý phản hồi"`), and `READY` (`"Sẵn sàng để xem"`) states. Record task ID and provider-finished/proposal-completed timestamps; a fast success completes in the same job flow without waiting for the 60s scanner. | **Pass** (`AiTaskFlowTest`, `GradingProposalServiceTest`, `workspace.test.tsx`) | **Pending** (User manual UI check) |
| 4 | Verify score summary and feedback appear; `"Áp dụng vào bản nháp"` stays disabled until the proposal is `READY`. No draft score is saved or published merely by receiving a result. | **Pass** (`workspace.test.tsx`) | **Pending** (User manual UI check) |
| 5 | Briefly interrupt network connectivity during polling, restore it, and verify the same request recovers with bounded backoff without another provider call. Confirm 401/403/404 auth/permission failures stop polling and surface once. | **Pass** (`workspace.test.tsx` — `recovers after a temporary network error without spamming duplicate error alerts` & `stops polling on 403 error and surfaces the error once`) | **Pending** (User manual UI check) |
| 6 | Exercise polling exhaustion using a test fixture/fake timer rather than waiting five minutes; verify delayed-status banner and `"Kiểm tra lại"` button resume checking the existing job without submitting a new AI job. | **Pass** (`workspace.test.tsx` — `surfaces delayed status and Kiểm tra lại button after repeated poll failures, resuming existing request without a new AI job`) | **Pending** (User manual UI check) |
| 7 | Use backend integration fixtures to simulate one callback failure, duplicate delivery, concurrent polling during callback, and a stale submission. Verify recovery/rejection, no version-churn on read polls, one provider call, and one usage charge. | **Pass** (`AiTaskFlowTest`, `GradingProposalResultHandlerTest`, `EvaluationVersionIntegrationTest`) | **Pass** (Verified via backend & PostgreSQL integration fixtures) |
| 8 | Navigate away/back or reload; existing proposal is visible, timers are cleaned up on unmount, no duplicate request is sent, and unsaved-input behavior follows the existing product flow. | **Pass** (`workspace.test.tsx` — `cleans up polling timers on unmount`) | **Pending** (User manual UI check) |
