# U07 AI Usage & Teacher Quota — domain-entities

> Current Construction revision 2026-10-13: 65 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 77. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## Owned entities và ràng buộc

- TeacherQuotaPolicy: educatorId/provider/model, hourLimit/dayLimit, globalPolicyRef, timezone, enabled, version. Numeric settings unresolved D04.
- UsageWindow: scope(actor/global), provider/model, windowType(HOUR/DAY), windowStart, reservedTokens, measuredTokens, uncertainTokens, version; unique scope/window.
- QuotaReservation: requestRef unique, fingerprint, actorId/connectionRef/model/targetRef/contentVersion, estimate/bound, state RESERVED/DISPATCHED/SETTLED/CANCELLED/UNKNOWN, timestamps.
- AiUsageRecord: requestRef/owner/source/provider/model/input/output/cache/reasoning nullable, provenance providerMeasured/estimated/externallyReported/unavailable, state/error/version.
- RequestCheckpoint: dispatch marker and provider request ID if available; duplicate worker does not dispatch when previous status unknown.

## Boundary

reserveTeacherQuota(actor, connection, model, requestRef, estimate); reconcileUsage; markUsageUnknown; listOwnUsage; getTeacherQuotaPolicy/setTeacherQuota(admin, policy, version) (read UC70 / update UC71; owner U07)

Không truy cập repository unit khác. IDs cross-unit là refs được kiểm qua ports, foreign key cụ thể chỉ tạo theo schema review; snapshot lịch sử không cascade delete. Schema naming là mục tiêu migration đề xuất, không khẳng định có trong database hiện tại.

## State và integrity

- U07 tên logic AI Usage & Teacher Quota; folder/package legacy chỉ để migration, không payment/wallet code mới. UC77 primary; quota read supports UC70; quota update supports UC71/U03.
- Institutional connection Teacher/SubjectManager ACTIVE đúng scope, không Student; quota policy hour/day Asia/Ho_Chi_Minh + cap toàn nền tảng và model/provider; mức số cần D04 xác nhận.
- Reserve estimate atomically trên durable DB windows trước dispatch; khóa theo thứ tự global/day/hour để tránh deadlock. Reservation dựa conservative bound theo input estimate + maxOutput cap model; estimate chưa đủ không dispatch shared request.
- requestRef unique + fingerprint(actor/target/version/connection/model): replay same returns same state; ref reuse khác reject. Quota hết fail closed, không fallback personal hoặc buộc mua token.
- Usage providerMeasured reconcile đúng request; reserved dư release chỉ khi số dùng known. Timeout/usage missing giữ UNKNOWN/PENDING_RECONCILIATION và remaining uncertainty, không tự hoàn quota hoặc retry tạo charge ẩn.
- Provider usage có thể vượt estimate: ghi true usage/overage, block future nếu hết limit và flag policy; không cam kết provider billing cap cứng. Reservation trước call hạn chế concurrency vượt estimate, không xóa actual usage.
- Personal usage record riêng không trừ shared grant. MCP tool calls không là token, externallyReported/unavailable không trừ quota API chung.
- Redis cache không là nguồn quota; numeric token không đơn giá đồng nhất giữa providers/models. Admin aggregates không raw prompt/keys/content.
