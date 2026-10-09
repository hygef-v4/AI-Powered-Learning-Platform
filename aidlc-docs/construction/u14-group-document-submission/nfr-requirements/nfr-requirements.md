# U14 Group Document & Submission - NFR Requirements

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Realtime và hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-01 | Sự kiện mục và tài liệu (giao/nhận/nhả/xong, nộp, ngưng giao) tới mọi thành viên đang mở Group Essay Workspace ≤ 2 s (p95). | BR-U14-20 |
| NFR-U14-02 | Chịu 100 kết nối SSE đồng thời trên một backend (không giữ thread mỗi kết nối). | NFR-003 |
| NFR-U14-03 | Heartbeat SSE mỗi 25 s; client tự kết nối lại, lần kết nối lại tải lại trạng thái đầy đủ. | BR-U14-22 |
| NFR-U14-04 | Mở tài liệu nhóm 50 mục p95 ≤ 800 ms; nhận mục p95 ≤ 200 ms; "Xong" p95 ≤ 1 s; tiến độ các nhóm của một bài p95 ≤ 800 ms. | NFR-003 |
| NFR-U14-05 | Tự lưu mục như U11: 10 s, nội dung mục ≤ 10 MB, gzip, 30 lần/phút/người. | NFR-U11-01, 02 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-10 | Nhận mục kiểm trạng thái trong dòng tài liệu đang khóa; hai người nhận cùng lúc chỉ một thành công, người kia nhận `409` "mục vừa được X nhận". | BR-U14-10 |
| NFR-U14-11 | Lưu nháp/Xong chỉ khi `claimedBy` = người gọi và `version` khớp. | BR-U14-11, 12 |
| NFR-U14-12 | Phần tử `revisions` chỉ thêm, không sửa; `submitted_snapshot` chỉ được ghi đè bởi `GroupSubmitter` khi có bản nộp mới. | BR-U14-31 |
| NFR-U14-13 | Nộp tay và tự nộp: tự nộp chỉ chạy nếu chưa có bản nộp sau lần sửa cuối; chạy lại scanner không tạo bản nộp trùng. | BR-U14-33 |
| NFR-U14-14 | Tự nộp khi hết hạn chờ 30 giây sau hạn để nhận lần lưu cuối; ngưng giao thì nộp ở lượt quét kế tiếp; cả hai chốt mục `CLAIMED` bằng `draftBlocks` đã lưu và chụp tài liệu trong cùng transaction. | BR-U14-04, 33, 36 |
| NFR-U14-15 | Chỉ trưởng nhóm hiện tại (U12) được giao mục và nộp; người được giao phải là thành viên đang hiệu lực; không ai thêm, xóa, đổi tên hay di chuyển mục. | BR-U14-02, 05, 30 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-20 | Kênh SSE chỉ mở cho thành viên hiện tại của nhóm (R5); kiểm quyền khi mở và khi đổi thành viên (kênh của người bị bỏ bị đóng). | BR-U14-03 |
| NFR-U14-21 | Nội dung mục kiểm và làm sạch SVG (U09) trước khi lưu/xong. | NFR-U09-13 |
| NFR-U14-22 | Mọi API kiểm quyền theo đối tượng ở server: thành viên R5 của đúng nhóm, trưởng nhóm hiện tại, người giữ mục, hoặc giảng viên chính của lớp (R3/R4); `ADMIN`, nhóm khác, tài khoản ngoài lớp nhận `404` và được audit. Bản nháp chỉ trả cho người giữ. | BR-U14-03, 50; current SRS contract |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-30 | Unit test mọi `BR-U14-xx`; test MockMvc quyền theo NFR-U14-22. | NFR-004 |
| NFR-U14-31 | Kịch bản integration test giao cho tester riêng (unit không viết): hai người nhận cùng mục; trưởng nhóm giao mục đang do người khác giữ; người giữ rời nhóm → nhả khóa; mọi mục xong không đổi trạng thái; trưởng nhóm nộp khi còn mục chưa xong (có cảnh báo); tự nộp tại hạn và khi ngưng giao lấy bản nháp của mục đang giữ, tài liệu `CLOSED`, không nộp trùng; mọi client đang mở tài liệu (sửa hoặc xem) hiện vòng chờ và nhận `GROUP_SUBMITTED` rồi chuyển trang; SSE nhận đúng sự kiện. | NFR-004; quyết định 2026-10-05 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung |
| SECURITY-05 | Compliant | NFR-U14-21 |
| SECURITY-08 | Compliant | NFR-U14-15, 20, 22 |
| SECURITY-15 | Compliant | NFR-U14-10…14 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
