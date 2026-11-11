# Gradebook export naming implementation

User approval: “Giúp tôi sửa theo plan” authorizes the reviewed requirements and their implementation/verification sequence.

Low-risk existing grading enhancement. Sequential flow: inspected workspace and requirements -> backend metadata/filename -> frontend download -> focused tests and checks. Skip separate stories/design/units/NFR/infrastructure stages: existing boundaries, no schema, dependency or infrastructure changes. Security and Resiliency retained; PBT disabled.

- [x] 1. Review export service/controller/exporter, browser API/dialog and connected tests.
- [x] 2. Return selected assignment title alongside rows from the same authorized export snapshot; preserve buildExportRows callers. Build a bounded safe Unicode filename and UTF-8 Content-Disposition.
- [x] 3. Add a shared authenticated download with filename metadata, reuse it in grading API and dialog. Server supplies the filename; retain Blob-only callers.
- [x] 4. Test selected/all/empty exports, Vietnamese/unsafe/reserved/long names, CSV/XLSX, header decoding and dialog download. Run affected Java/frontend suites, typecheck and scoped lint.
- [x] 5. Review diff and record evidence/limits. No deployment, migration or commit requested.

Acceptance criteria: [approved requirements](../../../inception/requirements/2026-11-10-gradebook-export.md). This is the scoped code generation and verification plan.

## Screenshot follow-up: Vietnamese workbook and assignment context

- [x] Reproduce the screenshots: frontend defaults to all assignments; backend emits English headers/raw status/boolean values, Gradebook sheet name and ID-based aggregate filename. Frontend regression fails1/16; backend regressions fail4/15 before implementation.
- [x] Localize exported presentation, preserve selected assignment context and give single-assignment/aggregate exports human-readable filenames. Preserve official-grade visibility and permissions.
- [x] Run connected regressions, frontend typecheck/lint and capture an actual workbook; record runtime verification and limits. Backend15/15 and frontend35/35 passed. Authenticated local UI downloaded the assignment-title workbook and an ID-free aggregate workbook; ZIP/XML assertions verified Vietnamese headers/statuses/sheet and data scope. No commit/push or VPS deployment in this follow-up.
