# U07 AI Usage & Teacher Quota — business-rules

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 77. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## Quy tắc nghiệp vụ

- **BR-U07-01**: U07 tên logic AI Usage & Teacher Quota; folder/package legacy chỉ để migration, không payment/wallet code mới. UC77 primary; quota read supports UC70; quota update supports UC71/U03.
- **BR-U07-02**: Institutional connection Teacher/SubjectManager ACTIVE đúng scope, không Student; quota policy hour/day Asia/Ho_Chi_Minh + cap toàn nền tảng và model/provider; mức số cần D04 xác nhận.
- **BR-U07-03**: Reserve estimate atomically trên durable DB windows trước dispatch; khóa theo thứ tự global/day/hour để tránh deadlock. Reservation dựa conservative bound theo input estimate + maxOutput cap model; estimate chưa đủ không dispatch shared request.
- **BR-U07-04**: requestRef unique + fingerprint(actor/target/version/connection/model): replay same returns same state; ref reuse khác reject. Quota hết fail closed, không fallback personal hoặc buộc mua token.
- **BR-U07-05**: Usage providerMeasured reconcile đúng request; reserved dư release chỉ khi số dùng known. Timeout/usage missing giữ UNKNOWN/PENDING_RECONCILIATION và remaining uncertainty, không tự hoàn quota hoặc retry tạo charge ẩn.
- **BR-U07-06**: Provider usage có thể vượt estimate: ghi true usage/overage, block future nếu hết limit và flag policy; không cam kết provider billing cap cứng. Reservation trước call hạn chế concurrency vượt estimate, không xóa actual usage.
- **BR-U07-07**: Personal usage record riêng không trừ shared grant. MCP tool calls không là token, externallyReported/unavailable không trừ quota API chung.
- **BR-U07-08**: Redis cache không là nguồn quota; numeric token không đơn giá đồng nhất giữa providers/models. Admin aggregates không raw prompt/keys/content.
- **BR-U07-09**: Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- **BR-U07-10**: expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- **BR-U07-11**: Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- **BR-U07-12**: No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Negative scenarios / acceptance

- Parallel reserves cannot exceed reservation limits; Student denied; same ref replay no second reserve; different fingerprint conflict.
- Timeout/crash unknown retains uncertainty; retry not dispatch again silently; measured overage stored/blocks future.
- Personal/MCP never subtract shared quota; no usage shown as 0 when missing; timezone hour/day boundaries and source switch explicit.
