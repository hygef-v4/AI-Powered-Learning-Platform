# Merge staging into the U05/U09/U15 convention branch

Status: six conflicts resolved, merged source verified and ready for the authorized local merge commit. No push.

## Scope and plan

Continue the user's existing merge on `fix/u05-u09-u15`: first parent `2440cbe4`, incoming `staging`/`origin/staging` and MERGE_HEAD `b91413ca`. Do not abort/restart the merge or replace unrelated staged incoming changes. Complete the local merge after verification; no push, deployment, live database migration or application restart.

- [x] Inspect both parent versions and retain staging behavior with the branch's convention names.
- [x] Resolve six files and check the affected callers, references and request/data contracts.
- [x] Reproduce the directly related automatic-source HTTP validation mismatch before correcting it.
- [x] Run final backend/frontend checks, document actual results and constraints and finish review; deliver through the authorized local merge commit without pushing.

## Resolutions

| File | Combined behavior |
|---|---|
| `AcademicsContractAdapter.java` | Keep role-specific repository/query names; make `mapClassReference` an instance mapper so every class-reference read includes staging's subject code through `subjectQuery`. |
| `GradingProposalTransactions.java` | Keep `requireAiSource`/`Evaluation.isBlankText`, while permitting staging's omitted source; retain request-reference checks and reject a provider unless the source is PERSONAL. |
| `ClassInfoForm.tsx` | Keep description-only editing and existing name/term preservation from staging, with `handleSave` and the correct dirty predicate. |
| `AiProposalPanel.tsx` | Keep staging's immediate automatic-source request without a source-picker dialog, using optional `buildAiRequest` and renamed scoring helpers. |
| `BatchActionDialog.tsx` | Keep immediate automatic-source batch requests, `requestProposals`/`buildAiRequest` and existing finalize/publish action dispatch. |
| `GradeDistributionSettings.tsx` | Keep staging's compact switch presentation, with `handleSave` and existing permission/error/busy guards. |

## HTTP validation regression

Independent review found an inherited staging mismatch directly blocking the merged UI: `GradingCommands.AiRequest.source` still had `@NotNull`, although the UI omits it for automatic selection. Both `@Valid` endpoints rejected the request before the service could select a source.

`GradingProposalApiContractTest` reproduced exactly two failures (single and batch: expected 200, actual 400); five other cases passed. Remove only the source annotation. Keep request-reference validation, required/validated nested batch request, provider/source checks, service authorization, locked grading/rubric rules and U06 source access/quota handling. Synchronize the grading OpenAPI schema and description; no field/route rename. The seven controller cases cover automatic selection, explicit PERSONAL compatibility, blank references and missing nested batch requests. These are HTTP binding/validation checks, not a replacement for authorization tests.

RED evidence: `.tmp/merge-staging-auto-source-red.log`. OpenAPI parsed with the already-installed `js-yaml`; requestRef remains required and source is optional. No dependency added.

## Verification and environment

TypeScript passes after the six conflict resolutions. Backend production/test compilation also passed. Initial full backend attempt could not reserve heap; the smaller retry reached 1,495 passing tests before exhausting its 192 MB metaspace. A concurrent retry and frontend run encountered native-memory allocation failures; the frontend reported 795 passing tests / 115 files but one worker error, so that attempt is not a clean pass. Preserve these logs and rerun heavy checks sequentially with bounded memory. Only this task's failed Vitest process was stopped; user applications and live servers remain running.

Final backend settings: JDK 17, UTF-8, heap 512 MB, metaspace 256 MB, SerialGC, two processors, 32 MB code cache, 64 MB compressed class space, tiered compilation level 1 and the existing Byte Buddy agent preloaded. Settings apply to verification commands; no product configuration change. Real PostgreSQL tests run in disposable containers, not the live database.

A subsequent frontend-only default-fork run completed 797 tests successfully across 115 files but a worker exited with Windows code 3221226505 before starting `SubmissionHistoryPage.test.tsx` (116 files expected). This remains a failed run, not a clean pass. Rerun with the installed Vitest's supported `--pool=threads --maxWorkers=1` to reduce process-spawn overhead; retain isolation and assertions. No product/test configuration edit or skipped test.

Final backend verify passed **1,804 tests, zero failures/errors/skips**, including the seven new HTTP cases and architecture; JAR packaged. Required real PostgreSQL guard passed (subject repository 31, notification delivery 2, no skips). Completed 2026-11-06T00:11:14+07:00. Evidence: `.tmp/merge-staging-backend-verified.log`, `backend/target/surefire-reports`.

Final frontend thread-pool run passed **799 tests / 116 files**, with isolation retained and no worker error. Full lint, TypeScript and isolated production build passed. Next used one worker (`CIRCLE_NODE_TOTAL=2` in the installed Next configuration) and a 768 MB Node heap; Vitest used one thread worker and the same heap cap. Restored original `tsconfig.json`/`next-env.d.ts` bytes after build and typechecked again successfully. Evidence: `.tmp/merge-staging-frontend-threads.log`, `.tmp/merge-staging-lint.log`, `.tmp/merge-staging-types-final.log`, `.tmp/merge-staging-build.log`, `.tmp/merge-staging-types-restored.log`. Earlier runs with worker errors remain recorded as failed attempts, not claimed as clean success.

Final scope checks: no unmerged index entries or conflict markers; no generated TypeScript configuration/crash evidence staged. Grading OpenAPI YAML parsed and added documentation links validated. Whitespace checks pass on the resolution/fix/documentation files; three untouched incoming files have pre-existing whitespace findings and are byte-identical to staging, so they were not reformatted. Final checks completed 2026-11-06T00:22:23+07:00. Local merge only; no push or operations change.

## Review and boundaries

Independent read-only review found no introduced defect in the six resolutions. A second read-only review of the corrected DTO, seven-case HTTP test and OpenAPI schema found no issue: teacher authorization, provider/source checks, U06 permissions/quota handling, request-reference and nested-request validation remain enforced. No Critical/Important findings remain.

Keep staging's two AI migrations and all incoming changes; do not run live migrations. Tracked cache metadata already present in staging is inherited, not generated by the resolution. Any whitespace findings on untouched incoming files are distinguished from new resolution changes; do not reformat unrelated staging files. Preserve historical AI-DLC records and the user's wildcard-import convention.

Enabled SECURITY-03/04/05/08/09/12/15 and RESILIENCY-04/06/10 scope remains unchanged. No new token, transport, data-access, retry or transaction behavior; authorization/validation/privacy/failure regressions are part of the backend suite. No new load/penetration/provider/browser acceptance claimed. PBT remains disabled.
