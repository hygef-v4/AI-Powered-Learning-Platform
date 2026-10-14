# U12 Group & Allocation — Code Generation Plan — revision chưa triển khai

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 54, 55, 57. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

## Authorization và trạng thái

User chỉ yêu cầu đồng bộ tài liệu Construction theo Inception. Đây là kế hoạch revision, không chạy Code Generation Part 2 hoặc đánh completed. Checklist/approval/code baseline giữ tại [plan cũ](../../historical/baseline-before-66-uc-2026-10-13/plans/code-generation-plan/u12-group-allocation-code-generation-plan.md); approval 05/10 không tự áp dụng scope mới. Không reset lịch sử code đã sinh thành chưa sinh; checklist dưới đây chỉ delta hiện hành.

## Paths cần review trước sửa code

- Existing backend owner/package: `backend/src/main/java/edu/aiplatform/groups`; frontend `frontend/src`, public contracts `backend/src/main/java/edu/aiplatform/contracts` nếu tồn tại; migration `backend/src/main/resources/db/migration`; tests `backend/src/test` và frontend test files.
- Exact classes/routes/migration numbers xác định qua repository inventory trước implementation; không invent files đã tồn tại. Legacy package/folder tên cũ không cho phép tiếp tục payment/code.
- Source-of-truth [current shared contract](../../current-srs-contract.md), unit design theo current folder. [Inception dependency](../../../inception/application-design/unit-of-work-dependency.md) là thứ tự hiện hành.

## Checklist delta

Truy vết giao diện/API/test: UC 57 — Create/Update/Delete Class Group sở hữu thao tác tạo/chia/sửa/xóa nhóm, thành viên, trưởng nhóm và duyệt/từ chối yêu cầu. UC 23 chỉ cung cấp điểm truy cập Teacher Class Detail → Students → Groups và Student Detail chỉ đọc; không thêm trang nhóm riêng. UC 54 là xem nhóm của sinh viên; UC 55 là gửi/hủy yêu cầu. Khi viết test phải bao gồm xóa nhóm chưa dùng, từ chối xóa nhóm có tài liệu/bản nộp và từ chối người không được giao dạy lớp. Checklist triển khai bên dưới vẫn chưa thực hiện.

- [ ] **Bước 1:** Inventory code/contracts/migrations baseline, xác định reuse/removal, mapping dữ liệu và exact paths; không drop records/snapshots trước backup/restore.
- [ ] **Bước 2:** Review DTO/status/public ports theo U12 owner; authorizations hiện hành, expectedVersion/idempotency và error projection; replace adapters fail-closed trước tích hợp thật.
- [ ] **Bước 3:** Migration owned entities/constraints/index/retention; preserve IDs/historical snapshots, validate old-to-new mapping trên copy database.
- [ ] **Bước 4:** Domain rules/services/transactions và recovery theo functional-design; dependent services gọi port, không repositories xuyên owner.
- [ ] **Bước 5:** API layer guards/validation/rate/CSRF, minimal projections, sanitized audit/outbox cùng owner transaction; external timeout/retry bounded.
- [ ] **Bước 6:** Frontend state/forms/query permissions theo entry UC hiện hành; conflicts/unavailable rõ; không client-only policy/security.
- [ ] **Bước 7:** Coder viết meaningful unit/service/API/UI tests của delta, negative permissions/state/duplicate/concurrency; không chỉ mirror implementation.
- [ ] **Bước 8:** Tester khác viết integration cases từ requirements/design và chạy adapter thật/sandbox/local mock phù hợp, phân biệt cases viết xong và runtime pass.
- [ ] **Bước 9:** Update deployment env/health/secret rotation và runbook; test migration/rollback/restore nếu unit tác động dữ liệu hoặc credentials.
- [ ] **Bước 10:** Review evidence/FR-story-UC traceability, fix/retest; user review trước next code stage, không tự thay milestones/nhân sự.

## Unit-specific implementation scope

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.

## Independent integration acceptance

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.

## DoD

Requirements owner, test-case owner, coder khác nhau theo nhóm; implementation readiness/compatibility và unresolved decisions xử lý trước enable feature tương ứng. No runtime pass/checklist tick chỉ vì docs viết xong; codes under workspace root, không aidlc-docs.
