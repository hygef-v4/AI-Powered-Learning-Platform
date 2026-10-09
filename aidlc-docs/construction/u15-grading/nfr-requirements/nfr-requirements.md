# U15 Grading - NFR Requirements

**Bản tài liệu 2026-10-09**: UC 37, 38, 39, 40 (chốt và công bố là luồng phụ của UC 38, 39) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-006, US-GRD-001, US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-01 | Chấm quiz luyện tập 200 câu khi nộp ≤ 2 s; Student thấy kết quả (nếu cài đặt quiz cho hiện) ≤ 5 s sau khi nộp. | BR-U15-10, 12 |
| NFR-U15-02 | Chốt hàng loạt 200 bài ≤ 5 s; công bố hàng loạt 200 bài ≤ 5 s. | US-GRD-005 |
| NFR-U15-03 | Sổ điểm lớp 200 người × 30 bài p95 ≤ 1 s; Submission Detail 200 bài nộp p95 ≤ 1 s. | US-GRD-004; UC 37, 40 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-10 | Điểm lưu `numeric(6,2)`, tính bằng `BigDecimal`; `0 ≤ score ≤ max_score` kiểm ở service và CHECK DB. | BR-U15-30 |
| NFR-U15-11 | Mọi thay đổi điểm kèm `version`; lệch → `409`; chốt hàng loạt bỏ qua mục lệch và báo. | US-GRD-005 S2 |
| NFR-U15-12 | `evaluations.history` chỉ thêm phần tử, không sửa phần tử cũ (một đường ghi `GradeWriter`); mỗi thay đổi điểm ghi lịch sử và audit trong cùng transaction. | FR-008 |
| NFR-U15-13 | Tạo đánh giá idempotent: một `evaluations` mỗi `(kind, attempt_id)` hoặc `(kind, group_document_id, account_id)`; gọi lặp không tạo điểm trùng. | Thiết kế |
| NFR-U15-14 | Mỗi đánh giá không phải `PRACTICE` có đúng một `class_id`; thời điểm công bố của (bài, lớp) suy ra từ `published_at` của các đánh giá đã công bố cùng `assignment_id`, `class_id`, không lưu bảng riêng. | BR-U15-03, 32 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-20 | API của Student chỉ trả điểm `PUBLISHED` và kết quả Practice của chính mình; không trả đề xuất AI, điểm tự chấm chưa công bố, lịch sử. | US-GRD-001 S2, US-GRD-004 |
| NFR-U15-21 | Xem bài nộp, chấm, sửa, chốt, công bố, sổ điểm chỉ cho giảng viên lớp (R3/R4) theo `class_id` của đánh giá; Chủ nhiệm môn không dạy lớp và Admin bị từ chối; vi phạm 404 và audit. | US-GRD-003 S4; current SRS contract |
| NFR-U15-22 | Phản hồi markdown hiển thị đã làm sạch; lý do sửa ≤ 1 000 ký tự văn bản thuần. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-30 | Unit test mọi `BR-U15-xx`; chấm quiz nhiều đáp án; đánh giá `PRACTICE` và quiz không lọt vào Submission Detail, sổ điểm, export; bài của môn chỉ hiện bài nộp của đúng lớp và công bố riêng từng lớp; Admin bị từ chối. | NFR-004, FR-030 |
| NFR-U15-31 | Kịch bản integration giao tester riêng (unit không viết): nộp Code Lab `GRADED` → điểm test `DRAFT` → chốt → công bố; chốt hàng loạt có mục lệch version; gọi port nộp lặp không tạo điểm trùng; lịch sử chỉ thêm; công bố bài của môn ở một lớp không làm lớp kia thành đã công bố. | NFR-004; quyết định 2026-10-05 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log phản hồi, điểm chi tiết |
| SECURITY-05 | Compliant | NFR-U15-22 |
| SECURITY-08 | Compliant | NFR-U15-20, 21 |
| SECURITY-15 | Compliant | NFR-U15-11…14 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
