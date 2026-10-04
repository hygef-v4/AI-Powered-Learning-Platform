# U12 Group & Allocation - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-01 | Lưu nhóm của lớp 200 sinh viên, 50 nhóm ≤ 1 s. | BR-U12-07 |
| NFR-U12-02 | `groupOf`/`leaderOf` p95 ≤ 20 ms (U14 gọi mỗi lần nhận mục/nộp). | GroupMembershipPort |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-10 | Lưu nhóm của lớp trong một transaction có khóa theo lớp (advisory lock) và khóa lạc quan `version` trên từng nhóm; lệch → `409`. | BR-U12-07 |
| NFR-U12-11 | Một sinh viên tối đa một nhóm đang hiệu lực trong lớp (kiểm trong transaction có khóa theo lớp); một yêu cầu `PENDING` mỗi nhóm. | BR-U12-02, 10 |
| NFR-U12-12 | Yêu cầu đổi trưởng nhóm không bị xóa (`REVOKE DELETE`); lịch sử thành viên (thêm, gỡ, đổi trưởng nhóm) nằm trong audit. | BR-U12-08, 32 |
| NFR-U12-13 | Chia ngẫu nhiên dùng `SecureRandom`; kết quả là bản xem trước, chỉ ghi khi giảng viên lưu. | BR-U12-05 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-20 | Chỉ giảng viên của lớp sửa nhóm/duyệt; sinh viên chỉ xem nhóm của mình; ngoài quyền `404`. | SEC-002 |
| NFR-U12-21 | Lý do yêu cầu 10-1000 ký tự, hiển thị dạng văn bản thuần. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U12-30 | Unit test mọi `BR-U12-xx`; chia ngẫu nhiên 7 người sĩ số 3 → 3/2/2, giữ nhóm cũ. | NFR-004 |
| NFR-U12-31 | Integration test: hai giảng viên lưu cùng lúc → một `409`; bỏ thành viên đang nhận mục thì U14 nhả khóa (khi có U14). | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | NFR-U12-21 |
| SECURITY-08 | Compliant | NFR-U12-20 |
| SECURITY-15 | Compliant | NFR-U12-10 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
