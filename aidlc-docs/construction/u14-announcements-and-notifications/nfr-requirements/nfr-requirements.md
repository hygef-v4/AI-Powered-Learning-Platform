# U14 Announcements & Notifications — nfr-requirements

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U14 Announcements & Notifications thuộc Iter 3, người code: Người 3; UC 63–65. Lấy thông báo lớp của U05 cũ và thông báo, email, nhắc hạn của U16 cũ. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 63, 64, 65. **Primary stories:** US-CNT-004, US-NTF-001; nhắc hạn của US-RPT-001. **Requirements:** FR-011, FR-019, FR-023.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U14-01 | Thông báo trong app tới người đang mở trang ≤ 5 giây sau commit của sự kiện. | US-NTF-001 |
| NFR-U14-02 | Class Announcements, danh sách thông báo p95 ≤ 300 ms, trang ≤ 50. | NFR-003 |
| NFR-U14-03 | Một sự kiện cho lớp 200 người học tạo xong thông báo trong ≤ 5 giây. | NFR-003 |
| NFR-U14-04 | Nhắc hạn gửi trong vòng 5 phút quanh mốc 24 giờ trước hạn. | FR-019 |
| NFR-U14-10 | Chống trùng bằng unique (sự kiện, người nhận, loại), unique email theo thông báo, dấu đã nhắc theo (bài, lớp, hạn). | BR-U14-10 |
| NFR-U14-11 | Email gửi ngoài transaction nghiệp vụ; lỗi email không rollback nghiệp vụ. | FR-011 |
| NFR-U14-30 | Markdown làm sạch khi hiển thị; email chỉ có nội dung tối thiểu và link cần đăng nhập. | SEC-003, FR-011 |
| NFR-U14-31 | Không log nội dung thông báo, địa chỉ email. | SEC-005 |
| NFR-U14-40 | Người code viết unit test cho mọi `BR-U14-xx`; tester viết integration test, gồm sự kiện lặp, chạm trần email, nhà cung cấp email lỗi, nhắc hạn khi đổi hạn, SSE trên hai instance, và phân quyền 4 role trên mọi endpoint. | NFR-004 |

## Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U14-31 |
| SECURITY-05 | Compliant | Kiểm độ dài, markdown |
| SECURITY-08 | Compliant | Giảng viên chính của lớp; người nhận chỉ xem thông báo của mình |
| SECURITY-15 | Compliant | Lỗi email, sự kiện không ảnh hưởng nghiệp vụ |
| Rule còn lại | N/A | SMTP thuộc U01; không có secret riêng |
