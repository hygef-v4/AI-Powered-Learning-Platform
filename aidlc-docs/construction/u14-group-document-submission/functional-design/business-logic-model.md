# U14 Group Document & Submission - Business Logic Model

## F1 - Khởi tạo tài liệu nhóm
1. U08 mở bài nhóm (`GROUP_ASSIGNMENT`) gọi `AssignmentLifecyclePort.onOpened` (U14 cài) → gửi việc `GROUP_DOC_CREATE` sau commit: với mỗi nhóm của lớp (U12) tạo một dòng `group_documents` cho bài đó, cột `sections` chứa một mục cho mỗi phần của khung (BR-U14-01). Tự nộp không cần lịch riêng: `GroupAutoSubmitScanner` đọc hạn của bài (F8).
2. Nhóm tạo sau khi bài đã mở: U12 gọi `GroupChangePort.onGroupCreated` (U14 cài) → việc `GROUP_DOC_CREATE` cho nhóm đó ở mọi bài nhóm đang mở của lớp.

## F2 - Mở tài liệu nhóm
1. Kiểm thành viên/giảng viên (BR-U14-03); trả tài liệu (phần chung + `publishedBlocks` + danh sách mục theo phần của khung + trạng thái mục + người giữ) + trạng thái tài liệu).
2. Mở kênh realtime của tài liệu nhóm (`groupDocumentId`).

## F3 - Trưởng nhóm giao mục
1. Mục là các phần của khung; không ai thêm, xóa, đổi tên hay di chuyển mục (BR-U14-02).
2. Giao mục cho một thành viên (BR-U14-05): trong dòng tài liệu đang khóa đặt `CLAIMED`, `claimedBy` = thành viên, `assignedBy` = trưởng nhóm; nếu mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13). Đẩy sự kiện tương ứng.

## F4 - Nhận mục
1. Thành viên nhận mục `OPEN` hoặc `DONE`: `CLAIMED`, `claimedBy`; `draftBlocks = publishedBlocks` (BR-U14-10).
2. Đẩy sự kiện `SECTION_CLAIMED`.

## F5 - Làm mục (popup che kín trang)
1. Người giữ mở mục trong popup che kín trang, popup hiện các heading trên nhánh của mục cùng nội dung của chúng và các block của mục, không hiện nhánh khác (chỉ trong popup này; các màn xem khác hiện toàn bộ tài liệu): `DocumentEditor` chế độ `STUDENT`; tự lưu `draftBlocks` (có version, như U11) (BR-U14-11).
2. Chỉ người đang giữ mục được lưu.

## F6 - Xong mục
1. Kiểm (BR-U14-12) → cập nhật mục, thêm phần tử vào `revisions`, → `DONE`, bỏ khóa; popup đóng. Đẩy `SECTION_DONE` kèm nội dung mới để Assignment Workspace của mọi người cập nhật.
2. Mọi mục `DONE` không đổi trạng thái tài liệu, không gửi thông báo (BR-U14-24).

## F7 - Nhả khóa
- Người giữ, trưởng nhóm hoặc giảng viên bấm Nhả: hiện cảnh báo bản nháp chưa Xong sẽ bị bỏ; xác nhận thì xóa `draftBlocks`, mục về `DONE` (đã có nội dung) hoặc `OPEN`; người giữ rời nhóm thì nhả tự động như vậy (BR-U14-13). Đẩy `SECTION_RELEASED`.

## F8 - Nộp
1. Trưởng nhóm nộp bất kỳ lúc nào khi bài còn nhận (BR-U14-30…32), hoặc `GroupAutoSubmitScanner` (mỗi phút) tự nộp khi bài quá hạn nhận cuối 30 giây hoặc đã ngưng giao (BR-U14-33).
2. Tự nộp: chốt các mục `CLAIMED` bằng bản nháp đã lưu gần nhất (thêm `revisions` theo người giữ, mục `DONE`); tài liệu → `CLOSED`.
3. Ghi `submitted_snapshot` (bản chụp bất biến, biên nhận), `submit_mode`, `submitted_at`; trong cùng transaction gọi `GroupSubmittedPort` (U15 cài: tạo/đặt lại dòng `evaluations` chờ chấm); sau commit phát `group.submitted` cho thông báo U16, đẩy `GROUP_SUBMITTED`.
4. Mọi client đang mở tài liệu khi tự nộp (đang sửa mục hoặc chỉ xem): khóa trang, người đang sửa gửi lần lưu cuối, hiện vòng chờ; nhận `GROUP_SUBMITTED` thì chuyển sang Submitted Assignment, không quay về Assignment Workspace (BR-U14-36).

## F9 - Cho U15
- `GroupSubmissionQueryPort` (BR-U14-40).
