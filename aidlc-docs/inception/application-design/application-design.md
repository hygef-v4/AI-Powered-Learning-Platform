# Application Design — 2026-10-13

> Revision Inception 2026-10-13 theo yêu cầu người dùng: SWR/SWD; hai loại bài Assignment/Quiz; Assignment cá nhân/nhóm, Draw.io tùy chọn; API chung theo quota giảng viên, API cá nhân và agent riêng qua MCP. Không mua credit, không Code Lab. Đây là revision tài liệu, Construction design/plans đã đồng bộ; chưa xác nhận triển khai code/runtime hoặc phê duyệt các quyết định còn mở.

## Architecture

Giữ Next.js desktop-first, Spring Boot modular monolith, PostgreSQL, Redis, RabbitMQ, worker cùng code, Google Drive và email. 15 logical modules, không 15 deployable. MCP là authenticated inbound adapter gọi domain services; direct provider API là outbound adapter. Không Judge0/PayOS.

## Invariants

1. Chỉ ASSIGNMENT/QUIZ. Assignment = INDIVIDUAL/GROUP × diagramEnabled; doc model chung.
2. Official grade/publication vẫn quyết định human; proposal/feedback không official.
3. Subject/class/object authorization chạy cho web/API/MCP; key/agent không nới scope.
4. Shared educator quota, không Student; personal API/agent không tự dùng shared account.
5. Provider usage đo được khác externallyReported MCP. Unknown không = zero.
6. Request gắn source/version/idempotency; stale output không overwrite. Autosave/submission không phụ thuộc AI.
7. Group sections/leader submit, rubric snapshot; quyền full-group AI còn D03.
8. Credentials riêng protected; Settings thường chỉ cấu hình non-secret, audit không raw key/prompt.
9. Quiz Practice/Group Graded tạm giữ D01/D02; Lesson extension không bắt buộc.
10. Construction/code/schema và diagrams hiện có chưa đồng bộ; không tự certify runtime.

## Ownership và validation

Xem components.md, services.md, component-methods.md, component-dependency.md và unit-of-work*.md. Security enabled subset giữ validation/auth/audit/secret; Resiliency giữ timeout/health/deploy; PBT disabled. Không thay phê duyệt giai đoạn cũ.

## Bổ sung hội đồng — current scope

Backup/restore evidence U03, policy activity U09, ActivityEvent U11 (U14 tích hợp), teacher review U15, email quan trọng/failure U16; minh bạch AI U07/U13. Không thêm unit hoặc đổi nhân sự. Xem [đặc tả hội đồng](../requirements/council-features-2026-10-13.md). D05 đã được người dùng yêu cầu bổ sung, D01–D04 còn mở. Đây là phạm vi tài liệu, chưa runtime evidence.

## Ranh giới sau gộp UC

Current catalog: 66 UC, 50 MVP stories + 1 extension, 15 logical units. UC owner là nơi điều phối trải nghiệm; data/service owner có thể khác. UC 70/U03 dùng quota U07, UC 58/U16 đọc backup U03, UC 37/U15 đọc activity U11/U14. Không repository access xuyên unit; thu gọn catalog không xóa requirement hoặc trộn trách nhiệm.
