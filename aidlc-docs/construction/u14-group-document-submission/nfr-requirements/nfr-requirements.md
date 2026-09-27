# U14 Group Document & Submission - NFR Requirements

## 1. Realtime và hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-01 | Sự kiện mục (nhận/nhả/xong/bình luận/nộp) tới mọi người đang mở tài liệu ≤ 2 s (p95). | BR-U14-20 |
| NFR-U14-02 | Chịu 100 kết nối SSE đồng thời trên một backend (không giữ thread mỗi kết nối). | NFR-003 |
| NFR-U14-03 | Heartbeat SSE mỗi 25 s; client tự kết nối lại, lần kết nối lại tải lại trạng thái đầy đủ. | BR-U14-22 |
| NFR-U14-04 | Mở tài liệu nhóm 50 mục p95 ≤ 800 ms; nhận mục p95 ≤ 200 ms; "Xong" p95 ≤ 1 s. | NFR-003 |
| NFR-U14-05 | Tự lưu mục như U11: 10 s, nội dung mục ≤ 10 MB, gzip, 30 lần/phút/người. | NFR-U11-01, 02 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-10 | Nhận mục bằng UPDATE có điều kiện trạng thái; hai người nhận cùng lúc chỉ một thành công, người kia nhận `409` "mục vừa được X nhận". | BR-U14-10 |
| NFR-U14-11 | Lưu nháp/Xong chỉ khi `claimedBy` = người gọi và `version` khớp. | BR-U14-11, 12 |
| NFR-U14-12 | `SectionRevision`, `GroupSubmission` bất biến (không UPDATE/DELETE với user `app`). | BR-U14-31 |
| NFR-U14-13 | Nộp tay và tự nộp: tự nộp chỉ chạy nếu chưa có bản nộp sau lần sửa cuối; một bản nộp mỗi lần kích hoạt (idempotent theo job). | BR-U14-33 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-20 | Kênh SSE chỉ mở cho thành viên nhóm và giảng viên lớp; kiểm quyền khi mở và định kỳ (khi đổi thành viên, kênh của người bị bỏ bị đóng). | BR-U14-03 |
| NFR-U14-21 | Nội dung mục kiểm và làm sạch SVG (U09) trước khi lưu/xong. | NFR-U09-13 |
| NFR-U14-22 | Bình luận văn bản thuần ≤ 2 000 ký tự. | SEC-003 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-30 | Unit test mọi `BR-U14-xx`. | NFR-004 |
| NFR-U14-31 | Integration test: hai người nhận cùng mục; người nhận rời nhóm → nhả khóa; tự nộp tại hạn; SSE nhận đúng sự kiện (2 client). | NFR-004 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung |
| SECURITY-05 | Compliant | NFR-U14-21, 22 |
| SECURITY-08 | Compliant | NFR-U14-20 |
| SECURITY-15 | Compliant | NFR-U14-10…13 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
