# Đồng bộ Inception theo 66 UC — 2026-10-13

> Historical synchronization record. Current scope and validation are superseded by [65-UC table synchronization](uc-table-65-sync-2026-10-13.md). Counts/checklists below retain the earlier revision evidence and are not current implementation approval.

Nguồn hiện hành: [bảng tiếng Anh](../../../docs/usecase_table.md), [catalog Inception](../requirements/use-cases.md). Request: “giúp tôi đồng bộ lại doc inception theo uc mới”. Documentation-only; giữ historical questions/plans/checkpoints, archive, Construction/code, diagram và Google Reports.

## Checklist

- [x] Rà scope/counts/catalog và primary ownership 66 UC; IDs 71/78 → 70, 80 → 58, 81 → 37.
- [x] Đồng bộ requirements/personas/acceptance và story traceability; giữ FR backup/activity/quota, không bỏ tính năng.
- [x] Đồng bộ orchestration/contracts/ports/dependency; UC owner khác data/service owner.
- [x] Sửa status D05 trong scope plan; D01–D04 còn mở, không tự chốt quota/Quiz Graded.
- [x] Xác định sources hiện hành và phân biệt plans lịch sử qua README; không sửa approval cũ.
- [x] Kiểm catalog/mapping/links/tables và ghi kết quả.

## Validation và giới hạn

Kiểm hai catalog đúng 66 IDs/names, một primary UC owner trong 3 maps; 50 MVP + 1 extension story giữ một primary story owner; 39 FR giữ ID. Kiểm references, tables/fences/local links của active artifacts. Chỉ sửa docs nên không chạy runtime tests; restore/activity/email/API compatibility chưa nghiệm thu.

Security subset đã chọn: kiểm quyền/input/secrets/audit/failure được giữ ở requirements/ports; resiliency subset đã chọn: timeout/safe failure/deploy được giữ. Các subset khác N/A theo state, PBT disabled. Không tuyên bố runtime compliant hoặc mở rộng policy.

**Kết quả:** PASS cho 24 active Markdown artifacts; hai catalog 66 UC khớp, 3 primary maps khớp, 51 stories/coverage/unit-story ownership khớp và 39 FR IDs unique. Tables/fences/local links hợp lệ. Không chạy runtime tests.
