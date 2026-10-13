# Component Dependencies — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

## Runtime (text alternative)

Web → Nginx → Spring Boot API → domain ports → PostgreSQL/Redis/Google Drive. Jobs sau commit → RabbitMQ → worker → provider API hoặc email. Agent ngoài → authenticated MCP adapter → cùng domain ports. AI direct dùng shared/personal connection; MCP agent gọi model riêng. Không PayOS, Judge0 hoặc thanh toán. RAG/YouTube chỉ extension.

## Data ownership

| Dữ liệu | Owner |
|---|---|
| Account/auth | U01 |
| Audit | U02 |
| File/job/Settings/backup manifests and status | U03 |
| Academic/enrollment | U04 |
| References/announcements, optional lessons | U05 |
| Rubric/MCQ versions | U06 |
| Quota reservations/usage windows/provenance | U07 |
| ASSIGNMENT/QUIZ lifecycle, submissionMode/diagramEnabled | U08; cấu hình chi tiết qua U09 port |
| Document/Quiz config | U09 |
| Individual attempts/submission/feedback references; ActivityEvent including group via U14 port | U11 |
| Groups/member/leader | U12 |
| Protected AI connections/MCP credentials/tasks | U13 |
| Group section versions/submissions | U14 |
| Evaluations/proposals/official grades | U15 |
| Notifications/exports/aggregates | U16 |

Credentials không cột plaintext accounts hoặc Settings value; database chi tiết cần Construction thiết kế mới. Unit gọi public port, không đọc repository khác. Giữ version/idempotency/audit.

## Trust

AI output, agent/tool input, files/XML và callback là untrusted. Authorize current actor/resource mỗi call. Không key/raw prompt trong audit, không hidden answer trong context Student. Shared quota backend-enforced, Student denied. MCP draft ghi expectedVersion, không official decision. Đồ thị dependency hiện hành ở unit-of-work-dependency.md (text/table), hình cũ chỉ historical.

## UC consolidation — 66 active UC

UC 70 Manage System Settings gộp 71/78: U03 primary; U07 giữ quota service/data và US-AIC-003. UC 58 gộp 80: U16 primary dashboard; U03 giữ backup service/data và US-OPS-001. UC 37 gộp 81: U15 primary review; U11/U14 giữ activity capture. UC 76 giữ riêng credentials/provider. Story count vẫn 50 MVP + 1 extension, 15 units; không bỏ chức năng hoặc acceptance vì gộp UC. Quiz Practice activity chỉ đọc trong scope/policy cho phép, không tự thêm Gradebook.
