# U09 partial rubric scores and AI recovery

User requests editable points on each checklist item (e.g. 0.50 out of 1.00) and diagnosis of continuing AI failure.

## Evidence
- Working tree clean at f4a19cf6; previous four local commits exist.
- Latest task dde70a84-3afd-440a-a79e-284ec8587526 failed with Gemini HTTP 400. Proposals still expose legacy generic errors.
- A fresh personal Gemini connection test on 25/10 17:26 passed with the same model/key. Therefore basic model/key connectivity works; the grading-specific request/runtime remains at fault.

## Design
- Add optional itemScoresByRubric to save/proposal payloads. Existing checkedItemsByRubric remains supported; an explicit item score overrides that item's legacy all-or-nothing value.
- Backend validates rubric/item ownership, non-null values, 0..item maximum and at most two decimal places; it computes the total. Client never supplies an authoritative total.
- Persist rubricItemScores in the existing grading_document JSON aggregate, including official-change history and group inheritance/reset. No SQL schema migration. Old snapshots load with an empty map.
- Numeric input per item, max label, validation and subtotal; checkbox remains a full-credit/zero shortcut. Saved scores reload and AI suggestions can prefill partial scores.
- Surface U06 task diagnostics for legacy generic failures. Detect a server that does not yet support partial scores and preserve the unsaved draft rather than send an unsupported DTO.

## Validation
Tests first for partial save, range/precision/unknown-ID rejection, JSON round-trip, legacy full-credit compatibility, UI partial entry/reload and diagnostic recovery. Then full frontend/backend suites, lint, typecheck/build and live UI inspection. Do not save, finalize or publish the real teacher grade while testing. Never change credentials or models.

## Verification results (2026-10-25)
- Frontend: 517 tests passed across 86 files; lint and production build (including TypeScript) passed.
- Backend: 1,354 tests, 0 failures/errors, 92 skipped infrastructure-dependent tests.
- git diff --check passed.
- Dev API started with local,worker and connected to PostgreSQL/RabbitMQ. Dev frontend reported ready on 3000.
- Live in-app browser received ERR_CONNECTION_REFUSED and a terminal HTTP request timed out. Sandbox isolation was a hypothesis at that time, not a verified root cause. No successful AI grading result was claimed and no teacher score was saved/published.
- Windows Terminal launch from the execution environment failed (file cannot be accessed), and opening the app terminal via Codex tool failed. Host dev launch is still required to finish end-to-end validation.

## Host continuation (2026-10-25)
- Resumed in the existing dirty `mcp-test` checkout; preserved the original source changes and generated Next.js instruction files. No branch switch, commit, push or deployment.
- Confirmed stale Java API/worker and Docker `platform-worker-1` simultaneously had RabbitMQ consumers. Stopped that Docker worker after verifying Compose project/service/path labels, and stopped only the verified repo Java/Next processes.
- Started `scripts/run-backend.bat` and `npm --prefix frontend run dev` in dedicated Orca host terminals. API PID 12432 started with `local,worker`; Next PID 47876 listens on 3000. RabbitMQ now has one application connection; PostgreSQL/Redis/RabbitMQ containers remain running.
- Orca browser successfully loads `http://localhost:3000` and redirects the protected evaluation URL to login. Localhost access is therefore working in this host session. No teacher session exists in the new Orca tab; requested user login without asking for a password in chat.
- Rechecked relevant frontend: 6 files / 43 tests passed. Backend: GradingServiceTest, GradingPromptSchemaTest, GradingProposalValidatorTest, EvaluationMappingTest and HttpProviderGatewayTest: 27 tests passed, zero failures/errors/skips. Backend log: ignored `.tmp/u09-host-backend-tests.log`.
- Existing Gemini connection remains ACTIVE with the original model and last test result `Đạt`. Historical failed task has no retained output schema/input payload, so its generic HTTP 400 alone cannot prove a schema cause.
- Live AI proposal and partial-score save/reload acceptance remain pending authenticated UI testing. Original evaluation is still DRAFT, score 0.00, version 7; no grade was saved, finalized or published in this continuation.

## Authenticated UI acceptance (2026-10-25, supersedes pending login above)
- User supplied a local test-account screenshot and requested login/testing. Teacher login succeeded through the Orca browser. Credentials were not added to project files or application logs.
- Requested a new proposal from the original evaluation UI using the existing personal Gemini connection/model. Task `ef9dfca2-cf13-4b0d-8b84-6c777ab1ac95` reached READY, handler_done=true, apply_result=APPLIED; proposal version 3 displays `Sẵn sàng để xem`, 10.00 points and Vietnamese feedback. APPLIED here denotes delivery to the proposal handler, not an official teacher grade.
- Created a separate local QA clone to avoid modifying the original grade: assignment `aaf2897a-1deb-4987-8349-f1783b6610bc`, submitted attempt `c378e0d9-76ed-4627-a9ae-a381f7c6bcb8`, evaluation `826f5c29-b5d1-4a32-9999-9313c242d8d3`, title `QA U09 partial scores 2026-10-25 host`. Copied assignment, locked rubrics, submitted content and pending evaluation in one database transaction with new IDs. The clone remains available for repeat testing.
- UI acceptance passed on the QA clone: enter 0.5 for an item worth 1, preview/subtotal 0.50, save draft and reload with 0.5 preserved. Enter 1.5 or 0.555: validation errors appear and save is blocked; database remains PENDING/version 0 until the first valid save.
- A second real UI proposal on the QA clone also succeeded: task `f4607f6d-71e6-426a-af6d-9dcfc9618642`, proposal `6e4eea09-6d45-49cc-bccd-c6f8318d03bd`, READY/APPLIED. The AI payload includes per-item scores. Clicking `Dùng đề xuất` filled the form with 10.00 points and Vietnamese feedback; changing the first item to 0.5, saving and reloading retained 9.50 total and all item scores/feedback. The proposal's usedAt is recorded.
- Final QA evaluation: DRAFT, score 9.50, version 2. Original evaluation remains DRAFT, score 0.00, version 7. No grades finalized/published, no notifications intentionally sent, no new AI connection or key/model changes.
- Runtime finding: duplicate consumers and stale processes were confirmed; after stopping them and running the current checkout on the host, the original HTTP 400 no longer reproduces in two real UI requests. Exact historical provider rejection remains unproven because its request/error body was not retained. Do not present speculative maxLength/nullability explanations as root cause. Argon 4 read-only consultation supplied hypotheses; successful runtime evidence takes precedence. No additional production-code change was necessary in this continuation.
- Evidence screenshots (local, ignored): `.tmp/u09-host-partial-score.png`, `.tmp/u09-host-ai-ready.png`. Scoped fresh checks remain 43 frontend + 27 backend tests passing, with git diff --check passing. Previous full-suite/lint/build results were not rerun or claimed as fresh.
- Scope complete: real AI request/result/apply and fractional input/validation/draft persistence verified on desktop Orca. Other U09 workflows, group/history runtime acceptance, mobile/cross-browser and official finalization/publication are outside this continuation's live checks.
