# Unit / Story / UC map — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

65 active UC; 50 MVP stories + 1 extension; 15 logical units

| Unit | Tên | Primary UC |
|---|---|---|
| U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 |
| U02 | Audit | 73 |
| U03 | File, Job, Event & Settings | 70, 71 |
| U04 | Subject, Class & Enrollment | 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 |
| U05 | Reference Content & Announcements | 15, 30, 33, 34, 36, 54, 55 |
| U06 | Rubrics & Subject Quiz Bank | 46, 56, 57 |
| U07 | AI Usage & Teacher Quota | 77 |
| U08 | Assignment/Quiz Lifecycle & Copy | 41 |
| U09 | Assignment Documents & Quiz Authoring | 35, 42, 45 |
| U11 | Individual Work & Quiz Submission | 18, 19, 20, 21, 22, 23, 24, 28, 29 |
| U12 | Group & Allocation | 16, 17 |
| U13 | AI Connections & MCP | 74, 75 |
| U14 | Group Workspace & Submission | 27 |
| U15 | Grading & Gradebook | 37, 38, 39, 40 |
| U16 | Reporting & Notification | 12, 58 |

## Story ownership

| Unit | MVP stories | Extension stories |
|---|---|---|
| U01 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 |  |
| U02 | US-AUD-001 |  |
| U03 | US-SET-001, US-OPS-001 |  |
| U04 | US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001 |  |
| U05 | US-CNT-001, US-CNT-002, US-CNT-004 | US-CNT-005 |
| U06 | US-QBK-001, US-QBK-002 |  |
| U07 | US-AIC-003, US-AIC-004 |  |
| U08 | US-ASM-001, US-ASM-008, US-ASM-009, US-ASM-010 |  |
| U09 | US-GRP-003, US-ASM-004, US-ASM-006 |  |
| U11 | US-ASM-003, US-ASM-012 |  |
| U12 | US-GRP-001, US-GRP-002 |  |
| U13 | US-AIG-001, US-AIG-002, US-AIG-003, US-AIC-001, US-AIC-002 |  |
| U14 | US-GRP-004, US-GRP-005 |  |
| U15 | US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001 |  |
| U16 | US-RPT-001, US-RPT-002, US-RPT-003, US-NTF-001 |  |

Catalog source: [use-cases.md](../requirements/use-cases.md). UC 24/27 include optional diagram editing; UC26 retired. UC38 primary U15, U13 hỗ trợ AI; UC29 primary U11, U13 hỗ trợ; UC79 retired. Retired IDs giữ trong migration table, không nằm trong active ownership.

## UC consolidation — 65 active UC

UC 70 View Settings và UC 71 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 không còn độc lập. UC 58 gộp 80 (U16 dashboard, U03 backup); UC 37 gộp 81 (U15 review, U11/U14 activity). UC 42 soạn Individual, UC 45 soạn Group (U09); Draw.io tùy chọn trong UC 24/27, UC 26 retired. UC 79 retired; UC 29 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.

## Truy vết chức năng đã gộp

| UC hiện hành | Requirement | Stories liên quan | Primary UC owner | Owner thực thi hỗ trợ |
|---|---|---|---|---|
| 70 View Settings / 71 Edit Setting | FR-033, FR-034, FR-038, FR-039 | US-SET-001, US-AIC-003, US-OPS-001 | U03 | U07 quota; U03 backup/activity retention settings |
| 70 View Settings / 71 Edit Setting (institutional API) | FR-012, FR-021, FR-037 | US-AIG-003 | U03 | U13 protected connection service; U07 usage/quota khi gọi AI |
| 58 View Admin Dashboard | FR-024, FR-038 | US-RPT-002, US-OPS-001 | U16 | U03 backup metadata |
| 37 View Student Submissions | FR-008, FR-019, FR-039 | US-RPT-001, US-INT-001 | U15 | U11 activity/submissions; U14 group workspace/submissions |

Story owner không buộc bằng primary UC owner: US-OPS-001 vẫn U03, US-AIC-003 vẫn U07, US-RPT-001 vẫn U16, US-INT-001 vẫn U15. Đây là cross-unit traceability, không tạo primary owner thứ hai cho UC.
