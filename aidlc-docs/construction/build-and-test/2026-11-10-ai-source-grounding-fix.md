# Source grounding correction — 2026-11-10

User reproduced an unrelated game quiz despite selecting history/reference material. The earlier implementation trusted a system prompt and rendered a warning, but still transitioned the mismatched questions to READY. That was insufficient enforcement.

## Implemented behavior

- New generation schemas require separate booleans requestMatchesSources and questionsMatchSources. Parsing still accepts legacy outputs missing these assessments and rejects wrong types.
- Either negative assessment with selected source documents prevents READY. Discard the output questions, persist the yellow warning and a pending retry marker; no mismatched proposal is available for acceptance.
- After the original result-delivery transaction commits, QuizSourceRetry schedules one source-only task. Its prompt contains the default instructions and selected document text; the original custom prompt is absent. Count/type/difficulty and explicit/automatic API source policy are preserved. Authorization, editable version, open-attempt guards, selectable/text access and normal U06 quota checks are repeated.
- Draft row locking and a deterministic retry request reference serialize repeated callbacks/owner polling. Queue publication and retry task metadata commit together. Submission errors roll back before recording FAILED in a new transaction; no rollback-only exception is swallowed. Another viewer does not trigger a billed task. Owner polling resumes a marker left after an interrupted after-commit callback; reads release their initial transaction before invoking retry.
- A second negative assessment fails with AI_SOURCE_MISMATCH and an empty proposal. Late original-task results/failures cannot overwrite the retry. Warnings remain visible after successful regeneration. Individual/bulk acceptance continues to require READY.

## Verification

Red: service regressions reproduced wrong-topic output becoming READY (`.tmp/ai-source-grounding-red.log`). Final backend:103 tests/six suites, zero failures/errors/skips, JAR package success (`.tmp/ai-source-grounding-final.log`). This includes11 real PostgreSQL repository cases and26 production-composition workflow cases with PostgreSQL/Redis/RabbitMQ; new cases cover lock contention/rollback, after-commit task creation and submission failure rollback using real Spring transaction propagation. No remote AI call.

Final frontend regression:44 tests/two files passed (`.tmp/ai-source-grounding-ui-regression.log`); no frontend production changes were required because pending/failed guards and yellow sourceWarnings already exist. Scoped application diff whitespace passed. Previous broader test results are historical, not reruns for this patch.

## Applying and limits

No migration, new endpoint or dependency. Conditional source-only regeneration can consume one additional AI task. Existing incorrect READY proposals are not silently rewritten or reassessed: discard and create a new request.

Semantic assessments are supplied by the model and can be wrong. This patch enforces negative assessments and removes the unrelated prompt from fallback generation; it does not prove every model claim is supported by sources. Authors still review questions. Live generation/UI acceptance remains with the user. Runtime applying is recorded in audit/state; restarting the normal launcher clean rebuilds classes, so packaged-JAR evidence predates that clean.

Security/Resiliency scope retained: current authorization/source checks, one retry, transactional job submission, row locks, finite failure and persistent warnings. PBT disabled. No commit/push/deployment.
