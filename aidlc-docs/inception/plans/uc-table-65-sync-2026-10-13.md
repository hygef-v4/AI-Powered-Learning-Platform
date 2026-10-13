# AI-DLC synchronization to the current 65-UC table

## Authorization and scope

Raw user request: giúp tôi đồng bộ doc aidlc theo bảng uc mới

Use [docs/usecase_table.md](../../../docs/usecase_table.md) as the source of truth. This is an authorized documentation-only revision of the existing workflow: no new implementation stage, no inherited approval, no automatic commit/push. Jira, application code/contracts/migrations, Google Reports and diagram files are outside this request.

## Applied changes

- UC26 diagram editing is part of UC24/27; XML and preview are retained. UC79 draft feedback is retired; UC29 only accepts submitted Practice Assignment feedback, with ownership/state/version guards for API and MCP writes.
- UC42 Create/Update/Delete Individual Assignment and restored UC45 Create/Update/Delete Group Assignment remain distinct authoring entries into one document model. Draw.io is optional in both.
- UC70 View Settings and restored UC71 Edit Setting remain separate. Institutional API UI is included; UC76/78/82 are retired/consolidated. U03 orchestrates; U07 owns quota and U13 owns protected credentials/test/revoke through public contracts.
- Names follow the table: Grade Submission With AI, Add/Update/Delete Rubric, Create/Update/Delete Subject Question and Manage Personal AI API.
- Educational YouTube links are core reference materials. Transcript/summary/embedding/advanced Lesson/RAG processing remains optional extension.
- Catalog, stories/personas/requirements, unit ownership, dependencies, current Construction designs and 15 code delta plans are synchronized. 50 MVP stories plus 1 extension and 15 units remain; no story, unit or human assignment is fabricated.
- D01 Quiz policy, D02 Group Practice, D03 group AI data scope and D04 numeric quota remain unresolved. Existing temporary defaults remain. Removal of draft feedback does not approve broader group AI access.

## Preservation

Previous active artifacts and state are preserved with SHA-256 hashes in [baseline snapshot](../../historical/baseline-before-uc-table-65-2026-10-13/README.md). Earlier dated sync reports retain their validation evidence and are explicitly superseded. Historical stage progress and approvals remain untouched; current delta plans remain unimplemented.

## Validation

- [x] Verify exact 65 active IDs/names/descriptions between the English table and Inception catalog.
- [x] Verify each active UC has exactly one primary owner across four current maps and unit headers; retired IDs are excluded.
- [x] Verify 50 MVP plus 1 extension story, current UC traces, 15 units, 15 code delta plans and 39 unchanged FR IDs.
- [x] Verify current scope guards, Markdown structure and relative link targets across 190 active artifacts.
- [x] Verify 192 snapshot hashes and unchanged historical state approvals.

Result: PASS, no detected mismatches. Source IDs, names and descriptions match exactly; every story has current UC trace and one implementation owner. No stale current 66-UC count, active retired-ID coverage, draft-feedback permission or YouTube-processing prerequisite detected in the checked source set. Old dated reviews and implementation summaries retain historical evidence and are not current scope sources.

Documentation validation only; runtime tests and external provider behavior are not certified by this revision.

## Extension compliance

Security subset 03/04/05/08/09/12/15 retains current authorization, input/URL/XML validation, protected/masked credentials, audit, scope and fail-closed behaviors. Resiliency subset 04/06/10 retains bounded retries, unavailable/unknown states, last-good backup and isolated restore requirements. Runtime infrastructure/compliance is not verified; other rules remain N/A under the project's reduced scope. PBT stays disabled.
