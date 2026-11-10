# Gradebook export — requirements and review

## Intent and workspace

User request: Giúp tôi check phần export gradebook excel nên hợp lý hơn với export nên để tên excel theo tên asignment

Simple enhancement to existing Java/Maven backend and React/TypeScript frontend. Existing AI-DLC project; scoped source inspection replaces missing conventional reverse-engineering paths. No application source changed at this checkpoint.

## Findings

- GradebookExportDialog overrides the downloaded filename with gradebook plus UTC date, regardless of selected assignment.
- GradebookController independently sends gradebook plus class UUID and Vietnam date in Content-Disposition.
- GradebookService already validates the selected assignment belongs to the gradebook, exports only the selected assignment when supplied, and hides unofficial scores/feedback.
- GradebookExporter writes real XLSX with inline text cells; CSV already protects formula-like values. XLSX currently has basic presentation without column widths, filters or a frozen header.
- Existing frontend export coverage checks format and assignment ID, but does not assert the saved filename.

## Proposed acceptance criteria

1. Selecting one assignment downloads `<assignment title>.xlsx`, e.g. `Bài tập tuần 1.xlsx`. Apply the same naming rule to CSV.
2. Preserve Vietnamese text; replace filesystem-invalid characters and control characters, trim trailing dots/spaces, bound filename length, and supply a safe fallback for an empty or reserved filename.
3. Exporting all assignments uses `Bảng điểm tổng hợp-YYYY-MM-DD.xlsx` (or CSV), without internal IDs. A scope containing only one assignment uses its title, including an empty class.
4. Align browser download naming and direct API Content-Disposition; encode Unicode safely and retain validation and permission checks.
5. Preserve export columns, official-grade rules, formula protection, filters and data scope. Visual XLSX redesign is outside this initial naming fix.
6. Cover selected/all exports, Vietnamese and unsafe titles, CSV/XLSX, and an empty class. Run affected tests and frontend typecheck/lint plus backend checks appropriate to changed code.
7. Screenshot follow-up authorized by the user: export Vietnamese headers, status labels, Có/Không values, Vietnam-local submission times and the sheet name Bảng điểm. In assignment view, preselect the currently viewed assignment in the export dialog. Keep an explicit all-assignment choice and XLSX as the default format.

## Scope and proposed sequence

- [x] Inspect workspace, export callers, implementation and existing regression coverage.
- [x] Document minimal requirements and concrete filenames.
- [x] Obtain requirements approval required by CLAUDE.md before implementation.
- [x] Plan and implement shared filename handling across backend response and frontend download, reusing existing helpers where available.
- [x] Verify focused regressions and record results.

Affected surfaces: GradebookController, GradebookService export metadata if needed, grading API download adapter, GradebookExportDialog, connected Java/frontend export tests. No dependency, schema or infrastructure change expected.

Security Baseline and Resiliency Baseline remain enabled: retain permissions, safe headers/filenames, export escaping and download error handling. Property-Based Testing remains disabled. No new extension opt-in requested.

## Requirements review

Approve the acceptance criteria above before proceeding to workflow planning.

A) Approve and continue.

B) Request changes to the proposed filenames or scope.

X) Other (describe below).

[Answer]: A — User: “Giúp tôi sửa theo plan”.
