# U09 AI grading rejection fixed — 2026-11-10

## Cause and correction

The failure was reproduced on the teacher's existing submission. Gemini returned zero item scores while also selecting a full-credit check in each of three active rubrics. The strict grading validator correctly rejected the contradictions with “Checks must match full-credit item scores”, previously reported as generic PROPOSAL_SCHEMA_MISMATCH.

Provider output requested two representations of the same judgment. The provider schema now requests item scores and feedback without checks, and the prompt explicitly says not to return checks. After deterministic empty-part merging, the existing normalizer derives full-credit selections from those scores. Points and evidence are not invented or relaxed.

The full proposal schema used by MCP remains unchanged. Explicit contradictory checks in submitted proposals still fail validation. Range/precision, exact total, rubric IDs, coverage, evidence, freshness, ownership and official-grade guards remain. Stored proposals still contain the complete checks structure consumed by the UI. Safe stage/class/diagnostic logging and a specific PROPOSAL_CHECKS_MISMATCH diagnostic remain for future failures; no raw values are logged.

Affected source: GradingPromptBuilder, GradingProposalTransactions, GradingProposalValidator and their tests. No frontend, migration or public REST payload change in this fix.

## Evidence

- Historical successful output replayed against the current rubric/validator: accepted. This ruled out a universally incompatible stored rubric; it did not establish the cause of past failures by itself.
- Local API/worker gracefully restarted after confirming zero queued/running AI tasks, with validation-stage diagnostics.
- Controlled teacher API reproduction: proposal c470fe46-00e1-452d-8c20-7742d9273ee6, task e97a3341-3fe7-4576-bd9a-bda9952af288, READY task delivered REJECTED. An in-memory read-only probe observed the transient output, reproduced ITEM_SCORES rejection and retained only anonymized text/shape. Three mismatches: checked=true with points0/max0.50, points0/max0.75, points0/max0.75.
- Regression first: the new schema test failed because checks were still requested. It now covers zero, partial and full credit, empty-part merging, provider rejection of extra checks, the unchanged full MCP schema and direct rejection of contradictory proposals.
- Focused tests:37 passed. Connected grading/AI suites: **210 tests / 38 classes, zero failures/errors/skips**, including PostgreSQL integration. Final two-class regression after the explicit strict-validation assertion:14 passed.
- Maven verify passed139 related tests and packaged the executable backend JAR. These counts overlap; they are not additive. Source was unchanged afterward; the final extra assertion only changed tests.
- Tested local API/worker restarted with the existing local/worker environment and compiled classes, health200, PID8496. The ignored launcher mirrors scripts/run-backend.bat without cleaning verified artifacts; it remains while the process runs.
- Live Gemini verification: proposal **23feea03-368d-4f6d-9e3b-97cc4643f28b**, version15, task95aa11f9-7335-4923-92d9-8f5db72548ca. At12:25:03 +07, task READY and delivery APPLIED; proposal API returned **READY**, score0.00, zero selected checks and **16 structured feedback items**. Delivery APPLIED means the proposal was accepted for review, not that the teacher's grade was saved.
- Before/after verification, evaluation stayed DRAFT, score0.00, version17, grade fingerprint2ed24f08c7e105cc7893f7b8760fbc35. No apply/save/finalize/publish request was sent.
- A separate authenticated Chrome tab opened the new proposal. Live DOM displayed “Đề xuất từ liên kết”, “Sẵn sàng để xem” and “Tạo lúc 12:24 10/11/2026”; alert region text was empty. The original tab was preserved. The optional element screenshot timed out; no screenshot success is claimed.
- Scoped diff whitespace check passed. Current frontend was unchanged, so its previous verification was not rerun.

## Artifacts and limits

Ignored evidence: `.tmp/grading-ai-probe.log`, `.tmp/grading-ai-anonymized-failure.json`, `.tmp/grading-ai-reproduced-runtime.json`, `.tmp/grading-ai-fixed-runtime.json`, `.tmp/grading-ai-rejection-red.log`, `.tmp/grading-ai-rejection-green.log`, `.tmp/grading-ai-rejection-verify.log`, `.tmp/grading-ai-connected-tests.log`, `.tmp/grading-ai-final-regression.log`, `.tmp/grading-ai-before-fix-state.json`, `.tmp/grading-ai-after-fix-state.json`.

Temporary read-only probe/replay source, raw historical replay input and copied classpath are removed after diagnosis. Failed provider output is still cleared by the existing task lifecycle; the preserved failure fixture replaces free feedback text with synthetic text while retaining types, bounds, IDs and scores.

Two real provider calls were made: one reproduction and one successful verification, using the configured Gemini connection/model. Other models were not tested live. No whole-backend2095-test rerun, grade write, migration, commit or push. Security/Resiliency retained; PBT disabled.
