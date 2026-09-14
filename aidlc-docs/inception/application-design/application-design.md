# Application Design - Consolidated

## 1. Quyết định kiến trúc

| Chủ đề | Quyết định |
|---|---|
| Backend | Spring Boot modular monolith với domain boundaries rõ |
| Frontend | Next.js desktop-first, gọi REST `/api/v1` |
| Contract | OpenAPI; backend authorization là nguồn chuẩn |
| Tác vụ dài | Job queue + worker, trạng thái, retry/backoff hữu hạn |
| File | Object storage abstraction; metadata/checksum/version trong relational DB |
| AI | Provider-neutral port/adapter, mock/sandbox contract tests |
| Realtime | MVP polling job status; SSE/WebSocket có thể bổ sung sau |

## 2. Các domain module

Identity & Access, Academic, Group, Content, Learning, Question Bank, Assessment, Submission, Grading, AI Orchestration, Code Execution, Reporting, Payment & Entitlement, Notification, Audit, File & Artifact và Job Platform.

Chi tiết trách nhiệm: `components.md`. Chữ ký interface: `component-methods.md`. Orchestration: `services.md`. Dependency/data flow: `component-dependency.md`.

## 3. Các invariants thiết kế

1. Quyền role và object scope được kiểm tra server-side cho mọi use case.
2. Chủ nhiệm môn chỉ quản lý phạm vi môn được giao; quyền đó không tự cấp quyền vận hành/chấm lớp.
3. Mỗi nhóm có đúng một leader; server kiểm tra leader ngay lúc nộp DOCX chung.
4. Submission/version là immutable sau khi nộp; chấm điểm chỉ tham chiếu đúng version.
5. Full Draw.io XML là artifact chuẩn; compact XML là derived artifact chỉ cho AI job được giảng viên yêu cầu.
6. AI không publish đề hoặc final grade; giảng viên giữ quyết định học thuật cuối.
7. Bài chung DOCX luôn chấm tay; AI chỉ có thể hỗ trợ phần cá nhân.
8. Payment entitlement chỉ phát sinh từ verified, idempotent provider event.
9. Audit không có application update/delete contract.

## 4. Luồng triển khai MVP

- Next.js và Spring Boot triển khai tách process/container nhưng cùng một sản phẩm modular monolith.
- Relational database giữ transactional data và artifact metadata.
- Object storage implementation có thể local-compatible trong development và thay bằng managed storage ở production.
- Worker process xử lý RAG, AI, Code Lab, notification, reconciliation và export.
- External systems luôn nằm sau ports/adapters để test bằng mock/sandbox.

## 5. Traceability

Thiết kế bao phủ 37 use case và 55 user story thông qua các module sau:

| Story domain | Module chủ đạo |
|---|---|
| IAM | Identity & Access |
| CAT | Academic |
| CNT/LRN | Content, Learning |
| GRP | Group, Submission, Grading |
| QBK | Question Bank |
| AIG | AI Orchestration, Content, Assessment |
| ASM | Assessment, Submission, Code Execution |
| GRD | Grading, AI Orchestration |
| RPT | Reporting |
| PAY | Payment & Entitlement |
| NTF/AUD | Notification, Audit |

## 6. Security và resiliency compliance

- **Security**: Compliant ở cấp application design với server-side object authorization, input/file/XML validation, provider isolation, secret redaction, immutable audit và payment integrity. Network/IAM cloud chi tiết chuyển sang Infrastructure Design.
- **Resiliency**: Compliant ở cấp application design với queue/worker, idempotency, retry/backoff, immutable source artifacts và manual fallback cho AI. RTO/RPO, multi-zone, backup/restore và alarms được chi tiết ở NFR/Infrastructure Design.
- **Property-Based Testing**: N/A vì extension đã tắt; unit/contract/system tests vẫn bắt buộc downstream.

Không có blocking finding tại Application Design.
