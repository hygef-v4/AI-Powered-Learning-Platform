# U07 AI Usage & Teacher Quota — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 33. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## UI entry và API đề xuất

- GET /api/v1/me/ai-usage; Admin quota commands via Settings boundary; TeacherQuotaPolicyPort/QuotaReservationPort/UsagePort.

## Component/state contract

- List/Detail/Editor phân theo quyền và entry UC hiện hành; query state loading/ready/empty/unavailable/error tách biệt.
- Form state draft/dirty/saving/saved/conflict/invalid; save gửi expectedVersion và requestRef khi có idempotent command; lỗi không xóa draft.
- State-changing requests không chỉ dựa ẩn nút; server authorize, version/policy errors cần refresh lại context trước retry.
- UI không phơi bày raw keys, hidden Quiz answers, unpublished grades hoặc dữ liệu ngoài scope.

## Hành vi riêng unit

- U07 tên logic AI Usage & Teacher Quota; folder/package legacy chỉ để migration, không payment/wallet code mới. UC33 primary; quota read supports UC14; quota update supports UC15/U03.
- Institutional connection Teacher/SubjectManager ACTIVE đúng scope, không Student; quota policy hour/day Asia/Ho_Chi_Minh + cap toàn nền tảng và model/provider; mức số cần D04 xác nhận.
- Reserve estimate atomically trên durable DB windows trước dispatch; khóa theo thứ tự global/day/hour để tránh deadlock. Reservation dựa conservative bound theo input estimate + maxOutput cap model; estimate chưa đủ không dispatch shared request.
- requestRef unique + fingerprint(actor/target/version/connection/model): replay same returns same state; ref reuse khác reject. Quota hết fail closed, không fallback personal hoặc buộc mua token.
- Usage providerMeasured reconcile đúng request; reserved dư release chỉ khi số dùng known. Timeout/usage missing giữ UNKNOWN/PENDING_RECONCILIATION và remaining uncertainty, không tự hoàn quota hoặc retry tạo charge ẩn.
- Provider usage có thể vượt estimate: ghi true usage/overage, block future nếu hết limit và flag policy; không cam kết provider billing cap cứng. Reservation trước call hạn chế concurrency vượt estimate, không xóa actual usage.
- Personal usage record riêng không trừ shared grant. MCP tool calls không là token, externallyReported/unavailable không trừ quota API chung.
- Redis cache không là nguồn quota; numeric token không đơn giá đồng nhất giữa providers/models. Admin aggregates không raw prompt/keys/content.

## UI acceptance

- Parallel reserves cannot exceed reservation limits; Student denied; same ref replay no second reserve; different fingerprint conflict.
- Timeout/crash unknown retains uncertainty; retry not dispatch again silently; measured overage stored/blocks future.
- Personal/MCP never subtract shared quota; no usage shown as 0 when missing; timezone hour/day boundaries and source switch explicit.
