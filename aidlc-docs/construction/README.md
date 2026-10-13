# Construction — nguồn hiện hành 65 UC

Revision 2026-10-13 đồng bộ Inception: 65 UC, 50 MVP + 1 extension story, 15 units, 39 FR IDs. Đây là detailed design/code planning, chưa implementation hoặc approval code mới.

1. [Current contract](current-srs-contract.md) và [shared infrastructure](shared-infrastructure.md).
2. Mỗi unit đọc functional-design, nfr-requirements, nfr-design và infrastructure-design tại đường dẫn trong bảng dưới.
3. [Kế hoạch đồng bộ](../inception/plans/uc-table-65-sync-2026-10-13.md) và plans/code-generation-plan/*.md là current delta chưa code.
4. [Inception ownership](../inception/application-design/unit-of-work-story-map.md) và [open decisions](../inception/requirements/revision-decisions-2026-10-13.md).

| Unit | Current responsibility | Folder giữ để không phá refs |
|---|---|---|
| U01 | Account & Access | [u01-account-and-access](u01-account-and-access/functional-design/business-rules.md) |
| U02 | Audit | [u02-audit-job-and-outbox](u02-audit-job-and-outbox/functional-design/business-rules.md) |
| U03 | File, Job, Event & Settings | [u03-file-and-artifact](u03-file-and-artifact/functional-design/business-rules.md) |
| U04 | Subject, Class & Enrollment | [u04-subject-class-enrollment](u04-subject-class-enrollment/functional-design/business-rules.md) |
| U05 | Reference Content & Announcements | [u05-content-material-rag](u05-content-material-rag/functional-design/business-rules.md) |
| U06 | Rubrics & Subject Quiz Bank | [u06-rubric-question-bank](u06-rubric-question-bank/functional-design/business-rules.md) |
| U07 | AI Usage & Teacher Quota | [u07-payment-credit](u07-payment-credit/functional-design/business-rules.md) |
| U08 | Assignment/Quiz Lifecycle & Copy | [u08-assessment-core-publication](u08-assessment-core-publication/functional-design/business-rules.md) |
| U09 | Assignment Documents & Quiz Authoring | [u09-question-type-authoring](u09-question-type-authoring/functional-design/business-rules.md) |
| U11 | Individual Work & Quiz Submission | [u11-attempt-submission](u11-attempt-submission/functional-design/business-rules.md) |
| U12 | Group & Allocation | [u12-group-allocation](u12-group-allocation/functional-design/business-rules.md) |
| U13 | AI Connections & MCP | [u13-ai-code-execution](u13-ai-code-execution/functional-design/business-rules.md) |
| U14 | Group Workspace & Submission | [u14-group-document-submission](u14-group-document-submission/functional-design/business-rules.md) |
| U15 | Grading & Gradebook | [u15-grading](u15-grading/functional-design/business-rules.md) |
| U16 | Reporting & Notification | [u16-reporting-notification](u16-reporting-notification/functional-design/business-rules.md) |

## Historical và implementation

[Snapshot trước revision](historical/baseline-before-66-uc-2026-10-13/README.md) giữ nguyên design/plan/checklists/approval cũ và hashes. Plans/questions/reviews ngày trước revision là historical, không source code mới. Gap review mô tả tình trạng trước sync, không bằng chứng tình trạng sau sync. Unit folders legacy U07 payment và U13 code không là active payment/code scope.

Backend/frontend/contracts/generated docs/Compose/database và Google Reports/drawio chưa được sửa trong request này. Approval/stage-progress cũ vẫn history; user chưa duyệt hoặc chạy delta code mới. Không commit/push tự động.
