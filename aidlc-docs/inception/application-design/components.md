# Components — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

| Unit | Tên | Primary UC |
|---|---|---|
| U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12 |
| U02 | Audit | 13 |
| U03 | File, Job, Event & Settings | 14, 15 |
| U04 | Subject, Class & Enrollment | 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 16, 17, 18, 19 |
| U05 | Reference Content & Announcements | 62, 63, 60, 61, 64, 58, 59 |
| U06 | Rubrics & Subject Quiz Bank | 36, 46, 47 |
| U07 | AI Usage & Teacher Quota | 33 |
| U08 | Assignment/Quiz Lifecycle & Copy | 34 |
| U09 | Assignment Documents & Quiz Authoring | 48, 35, 53 |
| U11 | Individual Work & Quiz Submission | 49, 50, 51, 52, 37, 38, 39, 40, 41 |
| U12 | Group & Allocation | 54, 55, 57 |
| U13 | AI Connections & MCP | 31, 32 |
| U14 | Group Workspace & Submission | 56 |
| U15 | Grading & Gradebook | 42, 43, 44, 45 |
| U16 | Reporting & Notification | 65, 66 |

## External ports

| Port | Adapter |
|---|---|
| AiProviderPort | Gemini/OpenAI/Claude direct APIs; normalized output/usage |
| AiConnectionPort | U13 shared/personal credentials, test/revoke, ownership |
| TeacherQuotaPort / UsagePort | U07 atomic reserve/reconcile, per-user/model/window and usage provenance |
| McpTransportAdapter | U13 authenticated tools into domain ports; not LLM execution by default |
| BackupStatusPort | U03 read-only backup metadata to U16; no archive/secrets |
| TeacherQuotaPolicyPort | U07 policy read/write for Admin Settings UC 14; U03 orchestrates |
| ActivityQueryPort | U11 scoped activity for U15 review UC 42, including U14 member events |
| FileStoragePort | Google Drive private file streams |
| MailPort | Brevo demo/production, Mailpit local |

Không PaymentPort, CreditPort, CodeRunnerPort hoặc Judge0. Giữ public contracts, không direct repository xuyên unit. U05 basic references MVP, Lesson/RAG optional.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## UC consolidation — 66 active UC

UC 14 View Settings và UC 15 Edit Setting là hai UC riêng, primary U03; bao gồm UI cấu hình quota/API chung. U07 giữ quota service/data; U13 giữ connection/provider/credentials bảo vệ và test synthetic qua public port. UC 76/78/82 (ID cũ) không còn độc lập. UC 66 gộp UC 80 (ID cũ) (U16 dashboard, U03 backup); UC 42 gộp UC 81 (ID cũ) (U15 review, U11/U14 activity). UC 35 soạn Individual, UC 53 soạn Group (U09); Draw.io tùy chọn trong UC 39/56, UC 26 (ID cũ) retired. UC 79 (ID cũ) retired; UC 41 chỉ feedback Assignment Practice đã nộp. 50 MVP + 1 extension story, 15 units; không tự phê duyệt D01–D04 hoặc runtime.
