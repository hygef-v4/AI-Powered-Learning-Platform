# Đồng bộ tài liệu UC57 — 2026-10-14

## Phạm vi

Yêu cầu: đồng bộ doc AI-DLC sau khi bổ sung và đổi tên UC57 thành Create/Update/Delete Class Group. Tiếp tục workflow tài liệu hiện có; không triển khai code, sửa diagram, Jira hoặc Google Reports. Không tái cấu trúc toàn bộ folder Construction theo unit mới.

## Kế hoạch và kết quả

- [x] Rà bảng UC, catalog, FR-025, stories, component map và thiết kế nhóm/lớp.
- [x] Đồng bộ tên UC57 và truy vết Inception; UC54 xem nhóm, UC55 gửi/hủy yêu cầu, UC57 giảng viên quản lý nhóm và xử lý yêu cầu.
- [x] Đồng bộ Construction: luồng nghiệp vụ, quy tắc, domain trace, giao diện, điểm truy cập từ UC23 và shared contract.
- [x] Bổ sung phạm vi UC57 vào code generation plan; giữ checklist implementation chưa thực hiện.
- [x] Kiểm tra tên UC57 giữa bảng và catalog, mapping UC của component và luồng nhóm, Markdown diff và liên kết tài liệu sửa.

## Quyết định

UC23 cung cấp Teacher Class Detail và tab Students; UC57 thực hiện chức năng nhóm trong phần Groups. Student Detail chỉ đọc vẫn thuộc UC23. UC53 soạn bài nhóm, UC56 làm/nộp bài nhóm, không trùng UC57. Quy tắc xóa nhóm chưa dùng và bảo toàn lịch sử nhóm đã có tài liệu/bản nộp giữ theo BR-U12-08. Tên folder và ID unit legacy trong Construction giữ theo bảng đối chiếu unit hiện hành.

## Workflow

Requirements và planning ở mức tối thiểu theo phạm vi tên/truy vết đã được người dùng yêu cầu. User Stories, design và code generation không khởi động stage mới; chỉ hiệu chỉnh tài liệu hiện có. Runtime/build/test, security/resiliency runtime evidence và PBT không áp dụng cho thay đổi tài liệu này; không tiến stage hoặc tự phê duyệt implementation. Tài liệu lịch sử giữ nguyên.
