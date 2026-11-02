# Integration test report — verification

Date: 2026-11-01. Branch: `integration-test-report`, starting baseline `staging@e54359e4`.

## Delivered specification

- 164 distinct cases, 15 unit sheets plus SecurityTest, 19 sheets total including Cover, Test Cases and Test Statistics.
- UC01–66 are mapped to cases; unit-level UC/FR/BR references, techniques, priority and execution methods are in Note. Traceability does not prove exhaustive business-rule coverage.
- Simple English case text, separate conditions/actions/expected results. Shared contract-stubs environment setting is not repeated in individual case preconditions.
- Initial feature rounds: Pending. SecurityTest: Not Run. No execution date, tester, reviewer, defect or Passed result invented.

## Checks completed

`verify-report.py`: PASS. Independently reopened the saved workbook and checked all 164 case rows against source JSON, 164 unique IDs, all 66 UC, 19 sheet names, nonempty test fields, blank execution identity/date/defect fields, unexecuted statuses, and no formula/error cells.

Initial combined summary: Passed 0, Failed 0, Pending 164, N/A 0, Total 164; execution and successful coverage 0.00%.

`build-report.mjs --verify-formulas`: PASS. In a disposable in-memory import, changed U01 Round 1 to Passed and verified per-round/global totals; changed Round 2 to Failed and verified independence from the Round 1 summary; changed SecurityTest to Pass and verified its mapping; restored all inputs in memory. The delivered file remained unchanged.

`build-report.mjs --render`: rendered 19 sheets. Reviewed all 19 previews at readable size. Corrected long catalog/summary/cover metadata wrapping and date display. Case text is wrapped in the original column widths; actual template fonts were used to calculate row heights, with maximum measured height 300.4 pt, below Excel's 409 pt limit. Case first/middle/last rows are included in all-row style/value checks. Template default green-italic placeholders were replaced with real cases using its regular black body-row style.

The original fonts, fills, borders, theme, existing style definitions, case-row styles, column widths and merged feature headers are preserved. Existing status validation lists are retained and extended to real case rows. The source contains no conditional-format rules. Only date-format variants and wrapping variants for long metadata/catalog fields were appended to existing styles. This avoids clipping without changing palette, fonts, borders or widths.

A fresh independent content reviewer found no Critical/Important findings. Source spot checks included material authorization/downloads, Redis MCP read progress and Quiz result masking. Snapshot behavior remains an explicit runtime expectation to check after open-Quiz edits; this document does not certify its implementation.

## Execution rulings

- Artifact-tool authored all cells/formulas. A final OOXML packaging step inserted those authored cells into the original worksheet/style shells to preserve template appearance, printer settings, cover image and native validations. No openpyxl authoring was used. The bundled library's public entry was loaded through createRequire after Windows denied a node_modules junction.
- Corrected affected template metadata #REF! references, incorrect shared Round 1 formulas for other rounds, expanded statistics ranges and obsolete invalid template defined names. Source template remains unchanged.
- Removed duplicated feature placeholder comments when copying sheets; retained cover and other reference sheet annotation/print resources. This avoids sharing one source comment part between 15 new case sheets.
- Summary intentionally uses Round 1, matching the source summary convention. SecurityTest has its original single status column; Not Run/Blocked map to Pending, Skip maps to N/A. The common environment text explains this.
- Localized metadata wrapping and date formats were necessary to avoid clipped long text/numeric date serials. Case-row font/border/fill styles and widths remain exact template equivalents.

## Limits

- These are written test specifications. Application cases were not run; no paid provider, real email, restore, account mutation or production fault was executed.
- Fault/clock/concurrency/contract-harness cases identify the necessary isolated QA tooling. Record actual generated IDs, deployed build/API base and evidence during execution; IDs in the workbook are fixture labels.
- Rendered with artifact-tool, not native Microsoft Excel. No native-Excel acceptance claim is made.
- No commit, push or merge. Existing unrelated Word report work was preserved.

## Cleanup and commit — 2026-11-02

The user requested removing temporary report-generation files and committing on the current branch without pushing. A fresh saved-workbook verification passed all 164 rows before cleanup. The final workbook blob is `74f0f2c74622964cd7d9407ee9b2c1c3c2b728fd`.

Removed intermediate workbook/inspection data, previews, generated JSON and one-off generation/verification scripts. Retained the final Excel workbook, coverage matrix, this verification record and approved workflow documents. Commands named above record completed checks; the temporary scripts are intentionally not retained as a supported rebuild toolchain.

No application code changed, so application test suites were not rerun for this documentation-only cleanup. The unrelated staged Word report is excluded from this commit. No push or merge is requested.
