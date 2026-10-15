# U07 AI Usage & Teacher Quota — Code Generation Plan — revision chưa triển khai

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 33. **Primary stories:** US-AIC-003, US-AIC-004. **Requirements:** FR-021/034/036.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u07-payment-credit` không quyết định chức năng hiện hành.

## Authorization và trạng thái

User chỉ yêu cầu đồng bộ tài liệu Construction theo Inception. Đây là kế hoạch revision, không chạy Code Generation Part 2 hoặc đánh completed. Checklist/approval/code baseline giữ tại [plan cũ](../../historical/baseline-before-66-uc-2026-10-13/plans/code-generation-plan/u07-payment-credit-code-generation-plan.md); approval 05/10 không tự áp dụng scope mới. Không reset lịch sử code đã sinh thành chưa sinh; checklist dưới đây chỉ delta hiện hành.

## Paths cần review trước sửa code

- Existing backend owner/package: `backend/src/main/java/edu/aiplatform/billing`; frontend `frontend/src`, public contracts `backend/src/main/java/edu/aiplatform/contracts` nếu tồn tại; migration `backend/src/main/resources/db/migration`; tests `backend/src/test` và frontend test files.
- Exact classes/routes/migration numbers xác định qua repository inventory trước implementation; không invent files đã tồn tại. Legacy package/folder tên cũ không cho phép tiếp tục payment/code.
- Source-of-truth [current shared contract](../../current-srs-contract.md), unit design theo current folder. [Inception dependency](../../../inception/application-design/unit-of-work-dependency.md) là thứ tự hiện hành.

## Checklist delta

- [ ] **Bước 1:** Inventory code/contracts/migrations baseline, xác định reuse/removal, mapping dữ liệu và exact paths; không drop records/snapshots trước backup/restore.
- [ ] **Bước 2:** Review DTO/status/public ports theo U07 owner; authorizations hiện hành, expectedVersion/idempotency và error projection; replace adapters fail-closed trước tích hợp thật.
- [ ] **Bước 3:** Migration owned entities/constraints/index/retention; preserve IDs/historical snapshots, validate old-to-new mapping trên copy database.
- [ ] **Bước 4:** Domain rules/services/transactions và recovery theo functional-design; dependent services gọi port, không repositories xuyên owner.
- [ ] **Bước 5:** API layer guards/validation/rate/CSRF, minimal projections, sanitized audit/outbox cùng owner transaction; external timeout/retry bounded.
- [ ] **Bước 6:** Frontend state/forms/query permissions theo entry UC hiện hành; conflicts/unavailable rõ; không client-only policy/security.
- [ ] **Bước 7:** Coder viết meaningful unit/service/API/UI tests của delta, negative permissions/state/duplicate/concurrency; không chỉ mirror implementation.
- [ ] **Bước 8:** Tester khác viết integration cases từ requirements/design và chạy adapter thật/sandbox/local mock phù hợp, phân biệt cases viết xong và runtime pass.
- [ ] **Bước 9:** Update deployment env/health/secret rotation và runbook; test migration/rollback/restore nếu unit tác động dữ liệu hoặc credentials.
- [ ] **Bước 10:** Review evidence/FR-story-UC traceability, fix/retest; user review trước next code stage, không tự thay milestones/nhân sự.

## Unit-specific implementation scope

- U07 tên logic AI Usage & Teacher Quota; folder/package legacy chỉ để migration, không payment/wallet code mới. UC33 primary; quota read supports UC14; quota update supports UC15/U03.
- Institutional connection Teacher/SubjectManager ACTIVE đúng scope, không Student; quota policy hour/day Asia/Ho_Chi_Minh + cap toàn nền tảng và model/provider; mức số cần D04 xác nhận.
- Reserve estimate atomically trên durable DB windows trước dispatch; khóa theo thứ tự global/day/hour để tránh deadlock. Reservation dựa conservative bound theo input estimate + maxOutput cap model; estimate chưa đủ không dispatch shared request.
- requestRef unique + fingerprint(actor/target/version/connection/model): replay same returns same state; ref reuse khác reject. Quota hết fail closed, không fallback personal hoặc buộc mua token.
- Usage providerMeasured reconcile đúng request; reserved dư release chỉ khi số dùng known. Timeout/usage missing giữ UNKNOWN/PENDING_RECONCILIATION và remaining uncertainty, không tự hoàn quota hoặc retry tạo charge ẩn.
- Provider usage có thể vượt estimate: ghi true usage/overage, block future nếu hết limit và flag policy; không cam kết provider billing cap cứng. Reservation trước call hạn chế concurrency vượt estimate, không xóa actual usage.
- Personal usage record riêng không trừ shared grant. MCP tool calls không là token, externallyReported/unavailable không trừ quota API chung.
- Redis cache không là nguồn quota; numeric token không đơn giá đồng nhất giữa providers/models. Admin aggregates không raw prompt/keys/content.

## Independent integration acceptance

- Parallel reserves cannot exceed reservation limits; Student denied; same ref replay no second reserve; different fingerprint conflict.
- Timeout/crash unknown retains uncertainty; retry not dispatch again silently; measured overage stored/blocks future.
- Personal/MCP never subtract shared quota; no usage shown as 0 when missing; timezone hour/day boundaries and source switch explicit.

## DoD

Requirements owner, test-case owner, coder khác nhau theo nhóm; implementation readiness/compatibility và unresolved decisions xử lý trước enable feature tương ứng. No runtime pass/checklist tick chỉ vì docs viết xong; codes under workspace root, không aidlc-docs.
