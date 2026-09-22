# Components

## 1. Kiến trúc tổng thể

MVP dùng modular monolith: frontend Next.js và một backend Spring Boot được chia thành các module domain có boundary rõ. Các tác vụ dài chạy qua job queue/worker. File nằm sau object-storage abstraction; relational database lưu metadata và dữ liệu giao dịch.

## 2. Thành phần frontend

| Component | Trách nhiệm | Interface chính |
|---|---|---|
| Web Shell | Điều hướng, session UX, layout desktop-first, accessibility | REST client, route guards mang tính UX |
| Learning UI | Lớp, nội dung, tiến độ, dashboard cá nhân | Learning API, Content API |
| Assessment UI | Làm bài, tự lưu, lịch sử nộp, xem kết quả | Assessment API, Submission API |
| Draw.io Canvas Adapter | Nhúng canvas, lấy/khôi phục XML đầy đủ, preview | Submission API; không tự rút gọn XML |
| Group Workspace UI | Nhóm, phần cá nhân, yêu cầu đổi leader, trạng thái composite và phản hồi | Group API, Submission API |
| Teaching Console | Quản lý lớp, nội dung, bài, rubric, copy giữa lớp, simulation, composite nhóm, chấm và báo cáo | Academic, Content, Assessment, Grading APIs |
| Subject Console | Học liệu/RAG gồm YouTube, ngân hàng, đề chung và template theo môn | Subject-scoped APIs |
| Admin Console | Tài khoản, role/scope, cấu hình AI, payment, audit | Admin APIs |

Frontend không phải nguồn quyết định authorization; ẩn/hiện UI chỉ hỗ trợ trải nghiệm.

## 3. Thành phần backend

| Module | Mục đích | Trách nhiệm cấp cao |
|---|---|---|
| Identity & Access | Danh tính và quyền | Kích hoạt, login/session, password, profile, role, subject/class/object scope |
| Academic | Môn, lớp và ghi danh | Subject, class lifecycle, assignment giảng viên, enrollment, mã mời Phase 2 |
| Group | Nhóm học tập | Group membership, đúng một leader, yêu cầu đổi leader, phần việc cá nhân |
| Content | Nội dung và học liệu | Nội dung lớp, file/YouTube source, caption/transcript, version và RAG lifecycle |
| Learning | Hành trình học | Quyền truy cập nội dung, progress idempotent, theo dõi lớp |
| Question Bank | Rubric và câu hỏi | Versioned rubric/question bank, preview, analytics Phase 2 |
| Assessment | Vòng đời bài đánh giá | Draft/review/publish, template/copy lineage, schedule, simulation policy, attempt snapshot và đề chung |
| Submission | Nháp và bản nộp | Autosave, immutable attempts, Draw.io full XML, group parts, generated composite version và receipt |
| Grading | Chấm và công bố | Deterministic/AI proposal cho bài cá nhân, manual composite grade, consistency rubric, manual per-student final decision |
| AI Orchestration | Tương tác AI | Provider-neutral ports, prompt context scope, jobs, quota, kill-switch |
| Code Execution | Code Lab | Sandbox job, limits, test visibility, execution results |
| Reporting | Truy vấn/báo cáo | Submission tracking, dashboard, exports, AI-vs-final analytics |
| Payment & Entitlement | Thanh toán/quyền lợi | Payment intent, verified webhook, reconciliation, entitlement idempotency |
| Notification | Thông báo | Outbox/job, template, recipient scope, delivery status |
| Audit | Bằng chứng bất biến | Security/business events, query by authorized admin, redaction |
| File & Artifact | Lưu trữ file | Object key, checksum, immutable version, scan status, signed access |
| Job Platform | Công việc nền | Enqueue, lease, retry/backoff, dead-letter, status and correlation |

## 4. Thành phần ngoài hệ thống

| Port | Vai trò |
|---|---|
| AI Provider Port | Generate draft, summarize, propose grade; implementation thay được |
| Object Storage Port | Put/get immutable object, signed access, checksum |
| Malware Scan Port | Quét upload trước khi phát hành/processing |
| Payment Gateway Port | Create checkout, verify webhook, query transaction |
| Mail/Notification Port | Gửi và nhận delivery status |
| Code Sandbox Port | Chạy code cô lập theo quota |

## 5. Boundary bắt buộc

- Module chỉ truy cập dữ liệu module khác qua application service/port đã công bố.
- Mọi object access nhận actor context và resource scope; controller không thay thế authorization service.
- Submission sở hữu bản nộp. Grading chỉ tham chiếu immutable submission version.
- Assessment sở hữu version/template/copy/publication policy; Submission giữ assignment/question snapshot tại thời điểm attempt bắt đầu.
- Content sở hữu transcript source/status; vector index chỉ giữ reference tới đúng resource version.
- Full Draw.io XML là artifact gốc. Derived compact XML thuộc AI job, có TTL/retention riêng và không thay đổi bản gốc.
- AI Orchestration không được publish assessment hoặc final grade.
- Payment chỉ cấp entitlement sau verified event và idempotency check.
