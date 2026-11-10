# U09 feedback formatting — 2026-11-10

Continue the grading UI follow-up in place without subagents. Preserve the existing visual system and plain-text/summary-array contracts.

1. [x] Add regression coverage for paragraph formatting, per-idea summary editing with retained line breaks, list limits and blank draft rows at save.
2. [x] Give general feedback a clear paragraph-formatting action and readable spacing. Render editable strengths/improvements as individual list entries with add/remove controls, retaining five-entry/500-character limits and historical nextSteps.
3. [x] Exclude blank editing rows from the save payload, preserving feedback content, item feedback, review metadata and grades.
4. [x] Run grading/document regressions, lint/typecheck/build and inspect desktop/mobile rendering using synthetic fixture data. Do not save live grades or issue AI requests.

Scope: grading feedback/header components, their hook save boundary and tests. No rich-text contract, automatic factual rewrite, backend change, migration, provider call, runtime restart, commit or push. Security/Resiliency retained; PBT disabled.

[Results and acceptance](../../build-and-test/2026-11-10-grading-feedback-format.md).
