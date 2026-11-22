# U09 unified AI grading evaluation

## Repository audit

- Maven grading tests and Vitest grading tests already covered the application contract. CI invokes them through the existing build commands.
- The Java harness already runs live Gemini, records the responses, and replays the same output through the current validator. Its case IDs, recorded attempts, rubric checks and provider errors are reusable.
- The Python DeepEval command previously treated saved Java validity as the end of deterministic evaluation. It exported another report set. The main overlap was status handling, expected-score/MAE reporting and independent report generation.
- Existing recordings and Java commands remain compatible. The Python entry point accepts its prior command spellings and reads legacy judge JSON as archived evidence.

## Implementation

- [x] Use one PowerShell and Python entry point to sequence existing automated suites, Java replay/live and GEval.
- [x] Join scenario records by dataset, provider, model, case ID and recorded attempt. Preserve raw artefacts and report validation, replay and judge outcomes independently.
- [x] Generate one JSON, workbook and Markdown set with optional baseline comparison and a shareable aggregate. Keep source and intermediate artefacts traceable.
- [x] Validate failure isolation, the live opt-in and offline routing with test-first regressions. Verify actual Java/Vitest, replay and GEval execution before release.

No application, dataset, Java harness, CI or deployment files are changed. Security and Resiliency are unchanged. Property-based testing remains disabled.

## Verified execution

| Command | Result |
|---|---|
| `run-tests.ps1` | 17 Python tests passed, including offline routing, judge failure isolation, live opt-in and report-only mode. |
| `-Mode full` | 277 Java and 226 Vitest passes, one intentionally skipped Java live test, one saved case replayed and three GEval scores returned. The replay identified a changed review flag in the older actor fixture. |
| `-Mode offline` | A recent saved actor attempt passed current Java validation and replay. Codex evaluated all three semantic metrics and reported a quality failure. |
| `-Mode replay -All` | 24 saved attempts across eight cases. Fourteen matched, seven historical provider errors stayed classified as provider errors, and three review flags changed from false to true with the same scores. |
| `-Mode report` | Rebuilt the 11-sheet workbook, compared a report with itself and imported a legacy judge report. |

Gemini Live was not invoked. The report marks the live layer as imported or not evaluated and retains the original archive path.
