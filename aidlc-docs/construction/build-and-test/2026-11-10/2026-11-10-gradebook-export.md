# Gradebook export naming — verification

Implemented approved assignment-title filenames for CSV and XLSX. Backend emits UTF-8 Content-Disposition; browser consumes the same filename through existing authenticated response handling. Export metadata and rows share the same authorized snapshot, including empty classes. Existing buildExportRows callers remain supported. After the screenshot follow-up, a single-assignment scope uses its title; multiple-assignment exports use Bảng điểm tổng hợp plus the Vietnam date without class IDs.

Filename handling replaces control/filesystem-invalid characters, removes trailing dots/spaces, falls back for blank/reserved Windows names, and limits titles to 60 Unicode code points (at most 240 UTF-8 bytes plus extension). No new dependency. Existing upload filename sanitizer was inspected but is not suitable for assignment titles: it strips path prefixes and does not handle Windows-invalid characters/reserved names. It remains untouched.

## Initial naming-change checks

- Backend: Maven test with GradebookExporterTest, GradebookAssemblyTest, GradebookControllerTest: **14 passed, zero failures/errors/skips**. Maven compiles source and tests. Use installed Maven 3.9.12 under the user's .m2/wrapper/dists with JAVA_HOME pointing to JDK 17; MAVEN_OPTS=-Xmx384m and -DargLine=-Xmx384m.
- Frontend: `npx vitest run tests/features/grading tests/shared/api --maxWorkers=1`: **183 passed / 20 files**. Final updated API/UI/shared-download coverage: `npx vitest run tests/features/grading/components/gradebook-layout.test.tsx tests/features/grading/api/navigation.test.ts tests/shared/api --maxWorkers=1`: **35 passed / 4 files**.
- Final `npm run typecheck` and ESLint on all six changed frontend source/test files: passed.
- Initial unrestricted frontend runs exhausted machine memory; successful reruns cap Node heap and worker count. Corrected one test's expected replacement-character count and its TypeScript callback annotation before final verification.

## Initial naming-change compliance and limits

Security: existing authorization, assignment membership validation, no-store/nosniff headers and spreadsheet formula protection retained; safe encoded filenames covered. Resiliency: shared auth refresh/error handling retained; missing/malformed filename header has fallback. Infrastructure/DR changes N/A. PBT remains disabled.

No full backend/frontend suite, production build, authenticated browser download or desktop Excel acceptance repeated. No live grades written, schema/dependency change, restart, deployment, commit or push. Very long titles are deliberately shortened. Visual workbook formatting is outside this approved naming change.

## Screenshot follow-up verification — 2026-11-10

- Reproduced the missing assignment-context default in the parent/dialog test (1/16 failed) and English headers/statuses, sheet and UUID fallback in backend tests (4/15 failed). These are separate symptoms from the earlier selected-assignment naming fix.
- Localized workbook headers/statuses, late flags and sheet title; submission times use dd/MM/yyyy HH:mm:ss in Asia/Ho_Chi_Minh. Score/feedback visibility remains limited to FINALIZED/PUBLISHED grades. No score data or permission policy changed.
- Assignment mode preselects the active assignment; student mode retains an all-assignment default. A single-assignment export derives its title even when called without an assignment ID or when there are no students. Multiple-assignment exports use an ID-free Vietnamese filename; browser fallback also avoids class IDs.
- Final connected backend15/15 (controller, assembly, exporter), frontend35/35 across4 files, frontend typecheck and scoped ESLint passed. No production source changed afterward.
- Restarted the identified existing local API/worker launcher; health200 and application startup confirmed. Startup applied two already-pending staging migrations: assignments20261109.2330 (allow group subject templates) and20261110.1200 (graded without attempt limit). No new migration was introduced by this fix.
- Authenticated Chrome UI downloaded SWD392 – Thiết kế hệ thống nhắn tin.xlsx to Downloads. Parsed its real XLSX ZIP/XML: sheet Bảng điểm, all9 Vietnamese headers,5 student rows for only that assignment, Đã công bố/Chưa nộp, Không flags and the same published0.00 score shown in the UI. Evidence metadata, without student identities, is in ignored .tmp/gradebook-vi-download-proof.json.
- Also downloaded Bảng điểm tổng hợp-2026-11-10.xlsx:20 rows across4 assignments; Vietnamese headers and no class ID in filename. Closed only the temporary verification tab and restored the user's original browser tab.
- User confirmed local usage and an existing team VPS deployment. VPS was not changed; these uncommitted source edits require a later release to appear there. No new provider call, grade mutation, dependency, commit/push, whole-suite rerun, production frontend build or desktop Excel opening. Existing Security/Resiliency retained; PBT disabled.
