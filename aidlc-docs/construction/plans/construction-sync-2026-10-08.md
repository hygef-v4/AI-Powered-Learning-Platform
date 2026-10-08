# Construction documentation synchronization - 70 UC

Request: "giúp tôi sửa lại doc construction theo inception và doc mới".

Source: current SRS-derived Inception (70 UC, 51 stories, 16 units), `docs/use-cases-and-screens.md` and latest `screen-flow (1).drawio`. This is an authorized revision of existing documentation; it does not generate code, change deployed contracts or approve new implementation.

- [x] Align authorization/assignments and remove subject-wide teaching grants.
- [x] Align payment administration, global payment history and announcement update/removal across functional, NFR and infrastructure designs.
- [x] Remove active avatar-update scope; retain generated-code facts and document implementation cleanup.
- [x] Rebuild current UC ownership, screen mappings and code-plan coverage; retain dated questions and completed-plan history.
- [x] Resolve missing documentation-source links and check all revised artifacts.
- [x] Update state/audit and resolve the ten review findings with evidence and remaining implementation work.

Existing enabled Security and Resiliency constraints apply to revised designs. Runtime checks are N/A to this documentation-only task; Property-Based Testing remains disabled. Original approvals refer to their prior baseline, not to these changes.

## Validation result

- 244 active Construction Markdown documents checked; 160 current design headers and 16 code plans accounted for.
- 70 UC and 51 stories each mapped to exactly one primary unit; headers match Inception ownership.
- Local Markdown links resolve; the original 21 missing source links were replaced. Three code-plan relative links discovered during validation were corrected.
- 64 historical stage-plan bodies and U03's original completed-plan body/checked steps preserved against Git HEAD; current implementation revision tasks remain unchecked.
- Fenced blocks are balanced; whitespace check passes. Screen labels/entries checked against the actual latest Drawio XML, including Admin Class List/Statistic/Sidebar and Teacher Uploaded Learning Materials.
- Only documentation changed; no application/contracts/migrations, diagram, questions or generated-code summaries changed. Runtime checks are N/A; no new approval or stage advancement.
