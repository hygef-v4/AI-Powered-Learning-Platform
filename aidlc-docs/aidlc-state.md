# AI-DLC State Tracking

## Project Information
- **Project Type**: Greenfield
- **Start Date**: 2026-09-12T15:04:50Z
- **Current Phase**: CONSTRUCTION
- **Current Stage**: Code Generation Part 1 - messaging redesign (2026-09-26): audit written in-transaction, cross-unit reactions via ports + jobs, 7 job queues, Redis/RabbitMQ renamed. Plans of U02, U05, U08, U11-U16 await re-approval; others approved.
- **Resume action**: Re-approve plans of U02, U05, U08, U11-U16, then start Code Generation Part 2 (wave 1: U01 and U02 in parallel; shared skeleton is U01 steps 1-6).

## Workspace State
- **Existing Code**: No
- **Programming Languages**: None yet (planned: Java 17 / Spring Boot 3, TypeScript / Next.js)
- **Build System**: None yet (planned: Maven, npm)
- **Reverse Engineering Needed**: No
- **Workspace Root**: `C:\Users\admin\Documents\GitHub\AI-Powered-Learning-Platform`

## Code Location Rules
- **Application Code**: Workspace root (NEVER in `aidlc-docs/`)
- **Documentation**: `aidlc-docs/` only
- **Structure Patterns**: See `construction/code-generation.md` in the AI-DLC rule details

## Extension Configuration
| Extension | Enabled | Decided At |
|---|---|---|
| Security Baseline | Yes | Requirements Analysis |
| Resiliency Baseline | Yes | Requirements Analysis |
| Property-Based Testing | No | Requirements Analysis |

**Phạm vi rule rút gọn (2026-09-24)**: Security chỉ áp dụng SECURITY-03, 04, 05, 08, 09, 12, 15; Resiliency chỉ áp dụng RESILIENCY-04, 06, 10. Các rule còn lại là N/A "ngoài phạm vi đồ án" ở mọi stage, không phải blocking finding. Xem `requirements.md` mục 12-13.

## Stage Progress

### INCEPTION
- [x] Workspace Detection
- [x] Reverse Engineering - skipped (greenfield)
- [x] Requirements Analysis
- [x] User Stories
- [x] Workflow Planning
- [x] Application Design (synchronized with unit designs 2026-09-25; global ERD removed - tables are defined per unit)
- [x] Units Generation (16 units)

### CONSTRUCTION
- [x] Functional Design - all 16 units
- [x] NFR Requirements - all 16 units
- [x] NFR Design - all 16 units
- [x] Infrastructure Design - all 16 units (+ `construction/shared-infrastructure.md`)
- [ ] Code Generation Part 1 (plans) - U02, U05, U08, U11-U16 updated for messaging redesign, awaiting re-approval
- [ ] Code Generation Part 2 (code) - not started
- [ ] Build and Test
- [ ] Operations (placeholder)

## Unit Progress

| Unit | Design stages | Code plan | Code |
|---|---|---|---|
| U01 Account & Access | Done | Approved | - |
| U02 Audit, Job & Event | Done | Updated, re-approval needed | - |
| U03 File & Artifact | Done | Approved | - |
| U04 Subject, Class, Enrollment & Learning Access | Done | Approved | - |
| U05 Content, Material & RAG | Done | Updated, re-approval needed | - |
| U06 Rubric & Question Bank | Done | Approved | - |
| U07 Payment & AI Credit | Done | Approved | - |
| U08 Assessment Core & Publication | Done | Updated, re-approval needed | - |
| U09 Question Type Authoring | Done | Approved | - |
| U10 Template, Copy & Simulation | Done | Approved | - |
| U11 Attempt & Submission | Done | Updated, re-approval needed | - |
| U12 Group & Allocation | Done | Updated, re-approval needed | - |
| U13 AI & Code Execution | Done | Updated, re-approval needed | - |
| U14 Group Document & Submission | Done | Updated, re-approval needed | - |
| U15 Grading | Done | Updated, re-approval needed | - |
| U16 Reporting & Notification | Done | Updated, re-approval needed | - |

## Open Items

- Chính sách hoàn tiền credit AI chưa chốt. Tài liệu PayOS hiện công bố API hủy link chưa trả và API lệnh chi riêng, chưa thấy API đảo ngược trực tiếp một payment đã `PAID`; cần quyết định phạm vi, điều kiện thu hồi credit đã mua và cách chuyển tiền trước khi thiết kế luồng hoàn tiền.
- Nhóm chỉ triển khai một MVP: 49 story và 77 use case. US-CNT-004, US-RPT-002 và US-RPT-003 thuộc MVP. Danh mục hiện hành đã bỏ `US-PAY-003`/`UC-PAY-02`; đối soát PayOS chỉ còn job tự động trong `US-PAY-002`. Không có kế hoạch triển khai Phase 2.
- Critical path (by plan steps): U01 → U04 → U05 → U08 → U09 → U10 → U11 → U15 → U16.
- VPS sizing suggestion: 4 vCPU / 8 GB RAM / 60 GB SSD (Judge0 included).

## History (summary)
- 2026-09-12..22: Inception completed and revised through several change requests (roles, Draw.io, group work, templates/simulation).
- 2026-09-24: Application Design corrected; unit split reworked from 17 to the current 16 units with four non-blocking waves; security/resiliency scope reduced; Construction started with U01.
- 2026-09-24..25: Per-unit design stages and code plans for U01-U16; decisions synced back to Inception (payment = AI credits, DRAWIO → DOCUMENT, no subject-wide assignments, versioning after retire, group work as a shared document, auto-grading on submit, automatic deadline reminders only).
- 2026-09-25: Inception clean-up: requirements/stories/personas/use-cases swept, application design files rewritten for 16 units, global ERD removed, dependency figure added.
- 2026-09-25: U03 simplified to avatar, material and document images (Draw.io XML lives inside documents, U09); dependency matrix and ports re-synced.
- 2026-09-25: Removed live references to the deleted global ERD; aligned the active story count and UC-ASM-01 trace. Clarified AI credit billing for U05 embedding and U13 generation, with U07 as credit owner and a separate system-busy response when AI quota is exhausted.
- 2026-09-25: Chốt thi thử mặc định 3 lượt, giảng viên chỉnh 1-10; người học được nhập DOCX vào lượt DOCUMENT đang làm sau khi xem trước; chưa tính điểm tổng theo hệ số. Mở lại quyết định hoàn tiền U07 để nghiên cứu PayOS.
- 2026-09-25: Nhóm xác nhận không thực hiện Phase 2; giới hạn dự án ở MVP (47 story, 74 use case). Các mục từng ghi Phase 2 chuyển thành ngoài phạm vi và giữ mã lịch sử để truy vết.
- 2026-09-25: Nhóm chọn lại ba tính năng số 2, 7, 8 của danh sách cũ cho MVP: thông báo/hỏi đáp lớp (US-CNT-004), dashboard cá nhân (US-RPT-002), xuất bảng điểm (US-RPT-003). Phạm vi lúc đó là 50 story/78 use case; sáu story còn lại ngoài phạm vi. Quyết định này thay thế dòng phạm vi 47/74 ở trên.
- 2026-09-25: Xóa 12 UC ngoài phạm vi khỏi danh mục use case theo yêu cầu; catalog UC lúc đó có 78 mục MVP, mã đã xóa không được tái sử dụng.
- 2026-09-25: Xóa 9 story ngoài phạm vi khỏi danh mục user story theo yêu cầu; catalog story lúc đó có 50 mục MVP, mã đã xóa không được tái sử dụng.
- 2026-09-26: Bỏ `UC-PAY-02` và `US-PAY-003` cùng thao tác admin đối soát/điều chỉnh credit thủ công; job tự đối soát được giữ trong `US-PAY-002`. Phạm vi hiện hành: 49 story/77 use case.
- 2026-09-26: Data model consolidated from 62 to 45 PostgreSQL tables (keep only tables that must stand alone, are listed by a use case, or tie to an external system; 1-1 data becomes columns). Five shared tables with owner-unit migrations and extension ports. Domain entities rewritten for all units; FD/NFR/Infra/plans/ERD synced. Account status `PENDING_ACTIVATION` renamed `PENDING`.
- 2026-09-26: Messaging redesign: audit INSERT in the business transaction (no audit queue); required cross-unit reactions (grading on submit, auto-submit on retire, group docs on open/new group, lock release on member removal, Code Lab score) via ports that enqueue U02 jobs; events only for U16 notifications. RabbitMQ: exchanges `jobs`, `platform.events`, `platform.realtime`; queues `jobs.scheduled`, `jobs.triggered`, `jobs.email`, `jobs.gemini`, `jobs.youtube`, `jobs.code`, `jobs.drive`, `jobs.payos`, `jobs.notification` (+ temporary `jobs.realtime.{instanceId}`). Redis: 12 key groups named by purpose; Gemini daily cost cap shared by U05/U13 via `AiBudgetPort`.
- Full chronological log: `audit.md`.
