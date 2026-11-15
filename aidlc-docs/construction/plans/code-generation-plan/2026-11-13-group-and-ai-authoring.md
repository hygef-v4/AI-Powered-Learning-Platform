# Group controls and AI authoring

User approved the seven proposals with “giúp tôi sửa theo đề xuất $ponytail:ponytail”. Reuse existing U05 enrollment batch, U12 versioned group save, U07 document model and U06 image worker. No new dependency or migration. Existing security/resiliency constraints apply; PBT disabled.

- [x] Diagnose current behavior and inspect callers/contracts.
- [x] Add searchable multi-select enrollment with selection of filtered results, keep partial failures visible and exclude current active enrollment.
- [x] Add filtered select-all for group members and explicit leader-change action using current group save/version guard.
- [x] Rename manager navigation to Đề mẫu and explain draft loss in release confirmation.
- [x] Accept validated PNG/JPEG sources and deliver private images to AI for generation and revision, preserving limits and ownership.
- [x] Support bounded document-native text formatting in generation/revision without HTML, preserving unchanged formatting.
- [x] Add/run focused regression tests, frontend lint/type/build and backend affected tests; inspect UI and document runtime limits.

Scope: academics EnrollmentPanel/useCandidates and student-candidate lookup; groups GroupFormDialog/ClassGroupsPanel; assignments navigation, source upload previews, draft service/schema/normalization/build; files purpose policy; groupdocs release dialog. No grade changes, publication, deployment or destructive student-data tests. Image references must resolve only from attached draft provenance, never arbitrary client file ids. Existing templates and old plain-text proposals remain compatible.
