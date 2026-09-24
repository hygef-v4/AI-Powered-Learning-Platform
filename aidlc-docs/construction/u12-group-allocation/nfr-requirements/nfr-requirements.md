# U12 Group & Allocation - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-01 | Lưu bộ nhóm 200 người học, 50 nhóm, 10 phần ≤ 1 s. | BR-U12-07 |
| NFR-U12-02 | `assigneeOf` p95 ≤ 20 ms (U14 gọi mỗi lần nộp phần). | AllocationPort |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-10 | Lưu bộ nhóm và phân công trong một transaction, khóa lạc quan `version` trên `GroupSet`; lệch → `409`. | BR-U12-07 |
| NFR-U12-11 | Partial unique: một người học một nhóm đang hiệu lực mỗi bộ; một phân công đang hiệu lực mỗi `(group, part)`; một yêu cầu `PENDING` mỗi nhóm. | BR-U12-02, 10, 20 |
| NFR-U12-12 | Lịch sử phân công và yêu cầu đổi trưởng nhóm chỉ thêm, không xóa. | BR-U12-22 |
| NFR-U12-13 | Chia ngẫu nhiên dùng `SecureRandom`; kết quả là bản xem trước, chỉ ghi khi giảng viên lưu. | BR-U12-05 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-20 | Chỉ giảng viên của lớp sửa nhóm/phân công/duyệt; người học chỉ xem nhóm của mình; ngoài quyền `404`. | SEC-002 |
| NFR-U12-21 | Lý do yêu cầu 10-1000 ký tự, hiển thị dạng văn bản thuần. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-30 | Unit test mọi `BR-U12-xx`; chia ngẫu nhiên 7 người sĩ số 3 → 3/2/2, giữ nhóm cũ. | NFR-004 |
| NFR-U12-31 | Integration test: hai giảng viên lưu cùng lúc → một `409`; chuyển phần giữ bài đã nộp của người cũ (với U14 khi có). | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | NFR-U12-21 |
| SECURITY-08 | Compliant | NFR-U12-20 |
| SECURITY-15 | Compliant | NFR-U12-10 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
