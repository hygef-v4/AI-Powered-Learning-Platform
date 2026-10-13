# Components — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

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

## External ports

| Port | Adapter |
|---|---|
| AiProviderPort | Gemini/OpenAI/Claude direct APIs; normalized output/usage |
| AiConnectionPort | U13 shared/personal credentials, test/revoke, ownership |
| TeacherQuotaPort / UsagePort | U07 atomic reserve/reconcile, per-user/model/window and usage provenance |
| McpTransportAdapter | U13 authenticated tools into domain ports; not LLM execution by default |
| BackupStatusPort | U03 read-only backup metadata to U16; no archive/secrets |
| TeacherQuotaPolicyPort | U07 policy read/write for Admin Settings UC 70; U03 orchestrates |
| ActivityQueryPort | U11 scoped activity for U15 review UC 37, including U14 member events |
| FileStoragePort | Google Drive private file streams |
| MailPort | Brevo demo/production, Mailpit local |

Không PaymentPort, CreditPort, CodeRunnerPort hoặc Judge0. Giữ public contracts, không direct repository xuyên unit. U05 basic references MVP, Lesson/RAG optional.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## UC consolidation — 66 active UC

UC 70 Manage System Settings gộp 71/78: U03 primary; U07 giữ quota service/data và US-AIC-003. UC 58 gộp 80: U16 primary dashboard; U03 giữ backup service/data và US-OPS-001. UC 37 gộp 81: U15 primary review; U11/U14 giữ activity capture. UC 76 giữ riêng credentials/provider. Story count vẫn 50 MVP + 1 extension, 15 units; không bỏ chức năng hoặc acceptance vì gộp UC. Quiz Practice activity chỉ đọc trong scope/policy cho phép, không tự thêm Gradebook.
