# AI grading totals computed by backend

Latest local proposal version7 still failed TOTAL after the earlier error-message fix. The provider schema required both individual rubric scores and an independently generated total. Normalization intentionally retained supplied totals for strict validation; model arithmetic could therefore reject otherwise usable scoring output. Raw rejected provider output was already cleared, so its exact incorrect number is unavailable.

## Change

Provider schema excludes `score`, alongside the previously excluded `checks`. Provider instructions explicitly delegate both to the system. Existing normalization derives the total from item scores, then the existing validator enforces complete coverage, known IDs, score range/precision, checklist agreement and feedback evidence. The full MCP proposal schema and its strict total validation remain unchanged. No item score is invented, rounded into validity or clamped by this fix; blank sections retain deterministic zero scores.

Production scope: `backend/src/main/java/edu/aiplatform/grading/service/GradingPromptBuilder.java`. Regression scope: `backend/src/test/java/edu/aiplatform/grading/service/GradingPromptBuilderTest.java`.

## Verification

- RED:2 failures across12 tests, reproducing redundant provider total requirement. Initial connected run caught2 test-fixture failures: the contradictory-check fixture also omitted the full MCP total, so it failed shape validation first. Restored its explicit total to isolate checklist validation; production validator unchanged.
- Final command: Maven test with `-Dtest=edu.aiplatform.ai.**,edu.aiplatform.grading.**,GradingDiagramIntegrationTest,ArchitectureTest`, UTF8,384MiB Maven/test heaps.349 tests pass,0failures/errors/skips; completed2026-11-10T17:33:39+07:00. Logs `.tmp/grading-total-red.log`, `.tmp/grading-total-green.log`, `.tmp/grading-total-final.log`.
- Regression exercises provider-schema validation and callback merge/normalization/validation with and without blank rubrics. Missing, unknown, negative, excessive and overprecision item scores still reject; independently supplied provider total also rejects at schema boundary. Existing MCP mismatch tests remain green.
- Refreshed local API/worker; health200 before UI request. No Docker restart or migration. Runtime `.tmp/grading-total-runtime.log`.
- One live Gemini request from a separate Chrome tab: task `b1ddf37e-e508-47cc-a700-3757ab92f6c3`, proposal `c58454d1-f6ca-4f69-a542-8ca24b4d4bb6`, version8, READY. Stored total7.50 equals the independently summed12 item scores; architecture2.00, class3.00, sequence2.50. UI shows the same values and no error dialog.
- Persisted evaluation `dbbf3c65-673f-49e9-8dbb-9fff6dcd1bbd` unchanged before/after: DRAFT0.00/version1, submission hash unchanged. Baselines `.tmp/grading-total-grade-before.txt` and `-after.txt`.

## Limits and compliance

No official grade save/finalize/publish, student/assignment edit, history rewrite, dependency, frontend change, commit/push or VPS update. Prior uncommitted fixes retained. Full backend suite/JAR and frontend checks not repeated; all connected AI/grading/architecture tests ran. Other providers not called live. AI scoring judgment can vary between calls; deterministic summation guarantees arithmetic agreement, not pedagogical correctness.

Security/Resiliency: validation, authorization, locked snapshots, safe errors and accounting retained; no sensitive content added to logs. Infrastructure/disaster-recovery rules N/A to this scoped schema change. PBT remains disabled. No subagents.
