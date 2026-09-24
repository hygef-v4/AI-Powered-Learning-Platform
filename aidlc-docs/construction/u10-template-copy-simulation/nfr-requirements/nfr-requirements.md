# U10 Template, Copy & Simulation - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U10-01 | Copy bài 200 câu (kể cả sao chép câu ngân hàng lớp) ≤ 5 s, đồng bộ. | BR-U10-11 |
| NFR-U10-02 | Diff hai version 200 câu p95 ≤ 1 s. | BR-U10-22 |
| NFR-U10-03 | Đọc chính sách thi thử p95 ≤ 20 ms (U11 gọi khi bắt đầu lượt). | BR-U10-35 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U10-10 | Copy chạy trong một transaction: hoặc có đủ bài nháp + câu sao chép + cấu hình + lineage, hoặc không có gì. | BR-U10-11…13 |
| NFR-U10-11 | Khóa chính sách thi thử bằng UPDATE có điều kiện `locked_at IS NULL`; sau khi khóa service từ chối sửa. | BR-U10-35 |
| NFR-U10-12 | Lineage chỉ thêm, không sửa, không xóa. | BR-U10-40 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U10-20 | Kiểm quyền cả nguồn và đích ở mọi thao tác copy/diff; ngoài quyền `404`, không lộ tên lớp đích. | US-ASM-010 S2 |
| NFR-U10-21 | Diff cho người học không tồn tại (chỉ giảng viên/Chủ nhiệm môn). | SEC-002 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U10-30 | Unit test mọi `BR-U10-xx`; diff các trường hợp thêm/bớt/đổi thứ tự/đổi điểm. | NFR-004 |
| NFR-U10-31 | Integration test copy lỗi giữa chừng không để lại dữ liệu; hai lượt đầu tiên bắt đầu cùng lúc chỉ khóa chính sách một lần. | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-05 | Compliant | Kiểm đầu vào chính sách |
| SECURITY-08 | Compliant | NFR-U10-20, 21 |
| SECURITY-15 | Compliant | NFR-U10-10 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
