# U07 AI Usage & Teacher Quota — tech-stack-decisions

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 77. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## Stack giữ theo dự án

Java 17/Spring Boot 3.5 modular monolith; Next.js 16/TypeScript, PostgreSQL/Redis/RabbitMQ, Nginx/Docker Compose, U03 Drive private và U01/U16 MailPort. Dependency lock/pin và actual APIs xác minh khi code; không chọn version SDK mới trong revision tài liệu.

- U07 tên logic AI Usage & Teacher Quota; folder/package legacy chỉ để migration, không payment/wallet code mới. UC77 primary; quota read supports UC70; quota update supports UC71/U03.
- Institutional connection Teacher/SubjectManager ACTIVE đúng scope, không Student; quota policy hour/day Asia/Ho_Chi_Minh + cap toàn nền tảng và model/provider; mức số cần D04 xác nhận.
- Reserve estimate atomically trên durable DB windows trước dispatch; khóa theo thứ tự global/day/hour để tránh deadlock. Reservation dựa conservative bound theo input estimate + maxOutput cap model; estimate chưa đủ không dispatch shared request.
- requestRef unique + fingerprint(actor/target/version/connection/model): replay same returns same state; ref reuse khác reject. Quota hết fail closed, không fallback personal hoặc buộc mua token.
- Usage providerMeasured reconcile đúng request; reserved dư release chỉ khi số dùng known. Timeout/usage missing giữ UNKNOWN/PENDING_RECONCILIATION và remaining uncertainty, không tự hoàn quota hoặc retry tạo charge ẩn.
- Provider usage có thể vượt estimate: ghi true usage/overage, block future nếu hết limit và flag policy; không cam kết provider billing cap cứng. Reservation trước call hạn chế concurrency vượt estimate, không xóa actual usage.
- Personal usage record riêng không trừ shared grant. MCP tool calls không là token, externallyReported/unavailable không trừ quota API chung.
- Redis cache không là nguồn quota; numeric token không đơn giá đồng nhất giữa providers/models. Admin aggregates không raw prompt/keys/content.

## Cost và extension

AI API tổ chức/cá nhân do bên tương ứng trả, không giả định free/đã có key; infrastructure dùng resources nhóm có. Full Lesson/RAG không prerequisite; mọi embedding/provider config là extension riêng. Numeric policies D04/retention chưa giả định. Không thêm unit/container cho từng module.
