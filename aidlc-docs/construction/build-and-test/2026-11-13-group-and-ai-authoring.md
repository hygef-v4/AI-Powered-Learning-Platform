# Group controls and AI authoring — verification

Plan: [approved scope](../plans/code-generation-plan/2026-11-13-group-and-ai-authoring.md).

## Delivered

- Searchable student multi-selection, select all filtered results, count, exclusion of current active enrollment. Reuses batch enrollment; failed rows stay selected with their server results. Candidate lookup and selection capped at 200, matching the existing batch parser; other account lookups remain capped at 20.
- Group member select-all only affects filtered eligible people; selections outside the filter survive. Explicit leader-change action edits only the leader through current versioned group-set save; archived classes remain read-only.
- Manager subject navigation renamed Đề mẫu. Release confirmation names the section/holder and explains draft loss and preservation of completed content. Release authorization unchanged.
- ASSIGNMENT_SOURCE accepts PNG/JPEG alongside TXT/DOCX/PDF. Existing 3-file, 2-MB-per-file, 6-MB-total and ownership/type inspection guards retained. Private worker image refs resolve through attached draft provenance; generation and revision both receive images. Frontend has local image thumbnails with object URL cleanup. Template format uploads remain DOCX; images are reference material, not OCR text.
- Structured formatting for generation/revision uses document-native alignment/run flags/colors only. Inline runs must preserve the exact plain text. New generation previews use the shared RunsView; unchanged revision formatting and rubric/version checks preserved. Lists/tables in revision accept whole-block styles. No HTML, CSS, dependency or migration added.

## Automated evidence

- Full Maven verify: **2,349 tests**, zero failures/errors/skips, success. Required integration guard passes five suites and 57 traced cases.
- After final alignment of candidate cap to the existing 200-row batch limit: **107 academics/identity tests** pass.
- Full frontend: **1,024 tests / 148 files**, all pass. Subsequent final cap delta: two enrollment selection tests pass.
- Frontend changed-feature ESLint and isolated TypeScript pass. Tests cover 35 candidate rows, filter selection, partial success, leader-only save/version, release warning, formatting preview/application/safety, image refs for both AI modes, foreign owner rejection and local object URL lifecycle.
- Production webpack build, TypeScript, prerender and traces pass using `.next-group-ai-check`. Original tsconfig restored afterwards; no test-only config retained in source.

Commands: Maven `-DargLine=-Xmx768m verify`; `scripts/assert-required-backend-tests.ps1`; `npm test`; scoped `npx eslint`; `npm run build` with `NEXT_DIST_DIR=.next-group-ai-check`.

## Build environment limit

The existing `.next/types` contains references to removed admin/mcp, ai/usage and submissions routes. Normal typecheck and the first build failed on these stale generated files. Automatic review rejected deleting that generated directory; it was not deleted or retried. Verification instead used fresh generated output and a temporary tsconfig including src/tests and that output, excluding the stale `.next` tree. Build changes to tsconfig were restored. A clean checkout/CI is not affected by that local stale directory; it still exists locally.

## Browser evidence / limits

- Authenticated teacher and manager sessions on localhost. Observed the separate leader-only dialog, member search/select-all mixed state, and manager multi-select empty state (all five available students already enrolled). No group/enrollment save or release was issued during post-change checks. Earlier diagnosis changed/restored the leader and left audit history as recorded separately.
- Inspected local mobile screenshots at approximately 464 CSS pixels. Did not repeat all desktop/theme/device combinations.
- The running backend still advertises `.pdf,.txt,.docx` for source files. It has not been restarted; new image acceptance and model formatting need a backend reload before live end-to-end acceptance. No paid model call, real release, student enrollment, commit, push or deployment performed.
- Security/resiliency: existing authorization, file ownership/limits, private image references, version checks, partial-failure feedback and quota-aware vision source selection retained. PBT remains disabled. No infrastructure/DR/topology change in scope.

Temporary logs: `.tmp/group-ai-verify.log`, `group-ai-limit-final.log`, `group-ai-frontend-final.log`, `group-ai-build-isolated.log`.
