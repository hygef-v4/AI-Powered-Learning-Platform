# Unit of Work — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

66 active UC; 50 MVP stories + 1 extension; 15 logical units

| Unit | Tên | Primary UC |
|---|---|---|
| U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 59, 60, 61, 62, 63 |
| U02 | Audit | 73 |
| U03 | File, Job, Event & Settings | 70 |
| U04 | Subject, Class & Enrollment | 13, 14, 31, 32, 47, 48, 49, 50, 51, 52, 53, 64, 65, 66, 67 |
| U05 | Reference Content & Announcements | 15, 30, 33, 34, 36, 54, 55 |
| U06 | Rubrics & Subject Quiz Bank | 46, 56, 57 |
| U07 | AI Usage & Teacher Quota | 77 |
| U08 | Assignment/Quiz Lifecycle & Copy | 41 |
| U09 | Assignment Documents & Quiz Authoring | 35, 42 |
| U11 | Individual Work & Quiz Submission | 18, 19, 20, 21, 22, 23, 24, 26, 28, 29, 79 |
| U12 | Group & Allocation | 16, 17 |
| U13 | AI Connections & MCP | 74, 75, 76 |
| U14 | Group Workspace & Submission | 27 |
| U15 | Grading & Gradebook | 37, 38, 39, 40 |
| U16 | Reporting & Notification | 12, 58 |

## Boundaries

15 unit là module logic trong một Spring Boot backend và worker cùng code, không 15 microservices. U10 vẫn retired. Giữ ID U07 nhưng scope mới Usage/Quota không Payment; folder Construction u07-payment-credit là lịch sử cần revision. U13 Connections/MCP, không Code Execution.

## Waves

| Wave | Units | Điều kiện |
|---|---|---|
| 1 — nền | U03, U02, U01, U04 | U03/U02 song song, U01 tích hợp auth, U04 theo U01. Iter 1 chỉ quản trị môn, lớp/enrollment chuyển Iter 2. |
| 2 — dữ liệu và cấu hình | U05, U06, U07, U12, U08 | U05/U06/U12 dùng U04; U07 dùng identity/settings, không payment. U08 cần rubric/bank, file và academic scope; tài liệu nguồn là tùy chọn, không Lesson prerequisite. |
| 3 — workspaces và chấm | U09, U11, U14, U15 | U09 theo U08; U11 theo U09; U14 thêm U12; U15 theo bài nộp. Manual workflow không chờ AI. |
| 4 — AI và kết quả | U13, U16 | U13 có thể mở khi identity/settings/U07 đủ, domain integrations qua neutral ports; không chờ Lesson/RAG. U16 cần owners/events, phần Admin Dashboard có thể làm Iter 1. |

Wave là dependency groups, không iteration hay barrier toàn wave. Tối đa 5 unit active theo slot và providers trực tiếp; chọn owner/reviewer theo planning, không có phân công Người 1–5 hoặc điểm effort tự động.

## Integration gates

| Gate | Evidence |
|---|---|
| G1 | Khung/auth/admin accounts/subjects/settings/audit, negative permissions, OTP; lớp triển khai Iter 2 |
| G2 | Tài liệu tham khảo, Quiz bank/rubric, groups, lifecycle/copy, quota không Student; không payment/Lesson gate |
| G3 | 4 tổ hợp Assignment, Quiz độc lập Lesson, autosave/submission/group, manual grades |
| G4 | 3 provider API, personal/shared/MCP, secret/revoke/quota/usage, feedback version và notifications |

## DoD và trạng thái

Requirement, independent test cases, implementation và evidence liên kết; runtime checks không tự đạt khi sửa docs. Construction design đã sync; actual code/contracts/migrations cần review lại, không sửa approvals cũ. Hình dependency PNG/drawio cũ chưa sửa, không nguồn current. Chuỗi dependency bằng bảng/text trong unit-of-work-dependency.md là nguồn hiện hành.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## UC consolidation — 66 active UC

UC 70 Manage System Settings gộp 71/78: U03 primary; U07 giữ quota service/data và US-AIC-003. UC 58 gộp 80: U16 primary dashboard; U03 giữ backup service/data và US-OPS-001. UC 37 gộp 81: U15 primary review; U11/U14 giữ activity capture. UC 76 giữ riêng credentials/provider. Story count vẫn 50 MVP + 1 extension, 15 units; không bỏ chức năng hoặc acceptance vì gộp UC. Quiz Practice activity chỉ đọc trong scope/policy cho phép, không tự thêm Gradebook.
