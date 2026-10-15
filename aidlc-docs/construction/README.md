# Construction — nguồn hiện hành 66 UC

Revision 2026-10-13 đồng bộ Inception: 66 UC, 50 MVP + 1 extension story, 15 units, 39 FR IDs. Đây là detailed design/code planning, chưa implementation hoặc approval code mới.

1. [Current contract](current-srs-contract.md) và [shared infrastructure](shared-infrastructure.md).
2. Mỗi unit đọc functional-design, nfr-requirements, nfr-design và infrastructure-design tại đường dẫn trong bảng dưới.
3. plans/code-generation-plan/*.md là current delta chưa code.
4. [Inception ownership](../inception/application-design/unit-of-work-story-map.md) và [open decisions](../inception/requirements/revision-decisions-2026-10-13.md).

| Iter | Unit | Current responsibility | Folder thiết kế hiện có (tên cũ) |
|---|---|---|---|
| 1 | U01 | Account & Access | [u01-account-and-access](u01-account-and-access/functional-design/business-rules.md) |
| 1 | U02 | Audit | [u02-audit-job-and-outbox](u02-audit-job-and-outbox/functional-design/business-rules.md) |
| 1 | U03 | Job and Settings | [u03-job-and-settings](u03-job-and-settings/functional-design/business-rules.md) |
| 1 | U04 | Subject Administration | [u04-subject-administration](u04-subject-administration/functional-design/business-rules.md) |
| 1 | U05 | Class & Enrollment | [u05-class-enrollment](u05-class-enrollment/functional-design/business-rules.md) |
| 2 | U06 | AI Connections | [u06-ai-connections](u06-ai-connections/functional-design/business-rules.md) |
| 2 | U07 | Assignment Authoring | [u07-assignment-authoring](u07-assignment-authoring/functional-design/business-rules.md) |
| 2 | U08 | Assignment Work | [u08-assignment-work](u08-assignment-work/functional-design/business-rules.md) |
| 2 | U09 | Assignment Grading | [u09-assignment-grading](u09-assignment-grading/functional-design/business-rules.md) |
| 2 | U10 | Quiz Authoring | [u10-quiz-authoring](u10-quiz-authoring/functional-design/business-rules.md) |
| 3 | U11 | Quiz Practice | [u11-quiz-practice](u11-quiz-practice/functional-design/business-rules.md) |
| 3 | U12 | Group Assignment | [u12-group-assignment](u12-group-assignment/functional-design/business-rules.md) |
| 3 | U13 | Reference Materials | [u13-reference-materials](u13-reference-materials/functional-design/business-rules.md) |
| 3 | U14 | Announcements & Notifications | [u14-announcements-and-notifications](u14-announcements-and-notifications/functional-design/business-rules.md) |
| 3 | U15 | Admin Dashboard | [u15-admin-dashboard](u15-admin-dashboard/functional-design/business-rules.md) |

## Historical và implementation

Snapshot trước revision, plans/questions/reviews cũ và folder thiết kế của 16 unit cũ đã xoá ngày 2026-10-15; cần xem lại thì dùng git history.

Backend/frontend/contracts/generated docs/Compose/database và Google Reports/drawio chưa được sửa trong request này. Approval/stage-progress cũ vẫn history; user chưa duyệt hoặc chạy delta code mới. Không commit/push tự động.
