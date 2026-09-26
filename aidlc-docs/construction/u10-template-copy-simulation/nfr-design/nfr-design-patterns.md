# U10 Template, Copy & Simulation - NFR Design Patterns

## P1 - Copy nguyên khối
- `AssignmentCopier.copy(source, target, kind, actor)` trong một `@Transactional`:
  1. `ScopeGuard` kiểm nguồn và đích (NFR-U10-20).
  2. U08 `createDraftFrom(source, targetOwner)` sao chép thành phần.
  3. Với thành phần trỏ câu/rubric ngân hàng cấp lớp nguồn: U06 `copyToClass` → thay tham chiếu (BR-U10-13).
  4. U09 `TypeConfigPort.copy`.
  5. Ghi lineage vào bài mới (qua `AssignmentExtensionPort` của U08, cùng transaction); audit sau commit.
- Mọi port gọi đồng bộ trong cùng transaction DB (cùng backend) → lỗi ở đâu cũng rollback (NFR-U10-10).

## P2 - Diff
- `AssignmentDiffer`: hướng dẫn qua `java-diff-utils` theo dòng; thành phần so khớp theo khóa (`bankItem.stableKey` hoặc hash `inlineDefinition`), đánh dấu `ADDED`, `REMOVED`, `MOVED`, `POINTS_CHANGED`, `CONTENT_CHANGED` (khác version ngân hàng hoặc khác hash); cấu hình loại bài so theo trường (NFR-U10-02).

## P3 - Khóa chính sách thi thử
- `UPDATE publications SET policy_locked_at = now() WHERE id = ? AND policy_locked_at IS NULL` (qua `AssignmentExtensionPort` của U08); 0 dòng nghĩa là đã khóa trước đó, vẫn thành công (NFR-U10-11).
- `SimulationPolicyService.update` từ chối khi `locked_at` có giá trị.

## P4 - Kết quả thi thử
- `SimulationResultCalculator` thuần: lọc lượt có điểm; `HIGHEST` = max, `LATEST` = lượt nộp muộn nhất có điểm, `AVERAGE` = trung bình `BigDecimal` làm tròn 2 chữ số; không có lượt có điểm → chưa có kết quả.
