# U15 Grading - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-01 | Tự chấm trắc nghiệm 200 câu sau khi nộp ≤ 2 s; người học có "hiện điểm ngay" thấy điểm ≤ 5 s sau khi nộp. | BR-U15-10, 12 |
| NFR-U15-02 | Chốt hàng loạt 200 bài ≤ 5 s; công bố một lượt phát hành 200 điểm ≤ 5 s. | US-GRD-005 |
| NFR-U15-03 | Sổ điểm lớp 200 người × 30 lượt phát hành p95 ≤ 1 s. | US-GRD-004 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-10 | Điểm lưu `numeric(6,2)`, tính bằng `BigDecimal`; `0 ≤ finalScore ≤ maxScore` kiểm ở service và CHECK DB. | BR-U15-30 |
| NFR-U15-11 | Mọi thay đổi điểm kèm `version`; lệch → `409`; chốt hàng loạt bỏ qua mục lệch và báo. | US-GRD-005 S2 |
| NFR-U15-12 | `GradeHistory` chỉ thêm (user `app` không UPDATE/DELETE); mỗi thay đổi điểm ghi lịch sử trong cùng transaction. | FR-008 |
| NFR-U15-13 | Job `GRADE_INIT` idempotent: một `Grade` mỗi `(targetKind, targetId, learnerId)`; job chạy lặp không tạo điểm trùng. | Thiết kế |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-20 | API người học chỉ trả điểm `PUBLISHED` của chính mình; không trả đề xuất AI, `autoScore` chưa công bố, lịch sử. | US-GRD-001 S2, US-GRD-004 |
| NFR-U15-21 | Chỉ giảng viên lớp sửa; ADMIN/Chủ nhiệm môn chỉ đọc; vi phạm → `404` và audit. | US-GRD-003 S4 |
| NFR-U15-22 | Phản hồi markdown hiển thị đã làm sạch; lý do sửa ≤ 1 000 ký tự văn bản thuần. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U15-30 | Unit test mọi `BR-U15-xx`; chấm trắc nghiệm nhiều đáp án; thi thử HIGHEST/LATEST/AVERAGE. | NFR-004 |
| NFR-U15-31 | Integration test: nộp → điểm tự chấm → hiện ngay; chốt hàng loạt có mục lệch version; job `GRADE_INIT` chạy lặp; `app` không sửa lịch sử. | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log phản hồi, điểm chi tiết |
| SECURITY-05 | Compliant | NFR-U15-22 |
| SECURITY-08 | Compliant | NFR-U15-20, 21 |
| SECURITY-15 | Compliant | NFR-U15-11…13 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
