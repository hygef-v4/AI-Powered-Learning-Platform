# U10 Template & Copy - NFR Design Patterns

**Bản tài liệu 2026-10-08**: UC 53, 54; primary stories: US-ASM-008, US-ASM-009, US-ASM-010. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## P1 - Copy nguyên khối
- `AssignmentCopier.copy(source, target, kind, actor)` trong một `@Transactional`:
  1. `ScopeGuard` kiểm nguồn và đích (NFR-U10-20).
  2. U08 `createDraftFrom(source, targetOwner)` sao chép thành phần.
  3. Câu ngân hàng cấp lớp nguồn: U06 `BankCopyPort.copyToClass` → thay tham chiếu; câu riêng của bài nguồn: `InlineQuestionPort` sao thành câu riêng mới(BR-U10-13).
  4. U09 `TypeConfigPort.copy` (nhân bản cấu hình và rubric từng câu/phần).
  5. Ghi lineage vào bài mới (qua `AssignmentExtensionPort` của U08, cùng transaction) và ghi audit trong cùng transaction.
- Mọi port gọi đồng bộ trong cùng transaction DB (cùng backend) → lỗi ở đâu cũng rollback (NFR-U10-10).

## P2 - Diff
- `AssignmentDiffer`: hướng dẫn qua `java-diff-utils` theo dòng; thành phần so khớp theo khóa (`lineage_id` của câu trong `questions`; câu riêng của bài so theo hash `definition`), đánh dấu `ADDED`, `REMOVED`, `MOVED`, `POINTS_CHANGED`, `CONTENT_CHANGED` (khác version ngân hàng hoặc khác hash); cấu hình loại bài so theo trường (NFR-U10-02).
