# U06 Rubric & Question Bank - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U06-01 | Tìm kiếm ngân hàng p95 ≤ 300 ms với ≤ 20 000 phiên bản mỗi môn, trang ≤ 50. | NFR-003 |
| NFR-U06-02 | Nhập file 500 dòng xử lý đồng bộ ≤ 10 giây . | BR-U06-40 |
| NFR-U06-03 | `getVersion` p95 ≤ 50 ms (đọc theo khóa chính). | NFR-003 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U06-10 | Unique `(stable_key, version_no)`; partial unique 1 `DRAFT` mỗi `stable_key`. | BR-U06-11 |
| NFR-U06-11 | Bản `ACTIVE`/`RETIRED` bất biến: service chặn sửa `definition`; test bảo đảm. | BR-U06-10 |
| NFR-U06-12 | Điểm lưu dạng `numeric(6,2)`, không dùng số thực dấu phẩy động. | BR-U06-30 |
| NFR-U06-13 | Nhập file: mỗi dòng một transaction; lỗi một dòng không ảnh hưởng dòng khác. | BR-U06-41 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U06-20 | Đọc xlsx có giới hạn: ≤ 5 MB, tỉ lệ nén tối thiểu (chống zip bomb), chỉ sheet đầu, ≤ 500 dòng, ô ≤ 32 000 ký tự; bỏ qua công thức (chỉ đọc giá trị). | SEC-003 |
| NFR-U06-21 | CSV UTF-8, có BOM hoặc không; dòng ≤ 64 KB. | SEC-003 |
| NFR-U06-22 | Đáp án đúng, test ẩn, `answerGuide` không bao giờ trả cho người học qua API của U06 (U06 chỉ phục vụ người quản lý; U11 tự lọc khi hiển thị). | SEC-002 |
| NFR-U06-23 | Sơ đồ Draw.io trong khung tài liệu kiểm bằng parser an toàn của U09. | BR-U09-35 |
| NFR-U06-24 | Kiểm quyền theo phạm vi ở mọi endpoint; ngoài phạm vi trả `404`. | SEC-002 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U06-30 | Unit test mọi `BR-U06-xx`, gồm từng loại câu và rubric; `score` với mục không thuộc rubric. | NFR-004 |
| NFR-U06-31 | Test nhập: 4 file mẫu hợp lệ, file có dòng lỗi, xlsx nén bất thường, CSV sai mã hóa. | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung đáp án |
| SECURITY-05 | Compliant | NFR-U06-20, 21, 23 |
| SECURITY-08 | Compliant | NFR-U06-22, 24 |
| SECURITY-15 | Compliant | NFR-U06-13 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
