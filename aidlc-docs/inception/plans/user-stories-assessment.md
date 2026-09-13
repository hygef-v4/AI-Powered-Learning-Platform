# Đánh giá nhu cầu User Stories

## Phân tích yêu cầu

- **Yêu cầu gốc**: Xây dựng MVP nền tảng học tập ứng dụng AI và duy trì quy trình AI-DLC có thể tái sử dụng.
- **Tác động người dùng**: Trực tiếp; sản phẩm có các luồng riêng cho người học, giảng viên, Chủ nhiệm môn và quản trị viên.
- **Mức độ phức tạp**: Phức tạp; phạm vi trải rộng trên học tập, đánh giá, AI, thanh toán, thông báo, bảo mật và khả năng phục hồi.
- **Các bên liên quan**: Người học, giảng viên, Chủ nhiệm môn, quản trị viên, đơn vị đào tạo và nhóm phát triển.

## Tiêu chí đánh giá được đáp ứng

- [x] **Ưu tiên cao - Tính năng mới hướng người dùng**: Toàn bộ MVP cung cấp chức năng mới mà ba vai trò sử dụng trực tiếp.
- [x] **Ưu tiên cao - Hệ thống nhiều persona**: Người học, giảng viên, Chủ nhiệm môn và quản trị viên có mục tiêu, quyền và hành trình khác nhau.
- [x] **Ưu tiên cao - Logic nghiệp vụ phức tạp**: Xuất bản nội dung, ghi danh, số lần làm bài, chấm AI có duyệt, webhook idempotent và phân quyền theo đối tượng đều cần tiêu chí chấp nhận rõ.
- [x] **Yếu tố trung bình - Nhiều điểm chạm và tích hợp**: Các hành trình đi qua frontend, backend, dữ liệu và dịch vụ AI, lưu trữ, thanh toán, email.
- [x] **Yếu tố trung bình - Rủi ro và kiểm thử**: Dữ liệu người học, điểm số, quyền truy cập và thanh toán cần đặc tả có thể kiểm thử và truy vết.
- [x] **Lợi ích**: Stories giúp chia phạm vi thành các lát cắt có giá trị, làm rõ quyền của từng vai trò và tạo cơ sở cho system test/e2e test.

## Quyết định

**Thực hiện User Stories**: Có

**Lý do**: Dự án đáp ứng nhiều chỉ báo ưu tiên cao và không thuộc nhóm thay đổi đơn giản có thể bỏ qua stage. Chi phí lập stories thấp hơn rủi ro hiểu sai các hành trình liên vai trò và các quyết định có tác động tài chính hoặc học tập.

## Kết quả mong đợi

- Bộ persona phản ánh bốn vai trò sản phẩm, gồm quyền cấp môn của Chủ nhiệm môn, và bối cảnh của đơn vị đào tạo.
- Stories ban đầu bao phủ FR-001 đến FR-014; revision ngày 2026-09-13 mở rộng tới FR-024 và giữ các ràng buộc NFR/SEC/REL có tác động đến hành vi quan sát được.
- Mỗi story có tiêu chí chấp nhận kiểm thử được và truy vết về yêu cầu nguồn.
- Các hành trình cốt lõi trở thành cơ sở cho thiết kế, phân rã unit và kiểm thử end-to-end/system.

## Tuân thủ extension tại bước đánh giá

- **Security Baseline**: Compliant - đánh giá xác định các luồng phân quyền, dữ liệu, audit và thanh toán cần stories/acceptance criteria kiểm thử được.
- **Resiliency Baseline**: Compliant - đánh giá xác định các tình huống lỗi dependency và phục hồi cần được thể hiện ở hành vi người dùng/hệ thống phù hợp.
- **Property-Based Testing**: N/A - extension đã bị tắt trong Requirements Analysis.
