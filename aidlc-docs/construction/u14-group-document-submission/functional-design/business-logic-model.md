# U14 Group Document & Submission - Business Logic Model

## F1 - Khởi tạo tài liệu nhóm
1. U08 mở bài `GROUP` gọi `PublicationLifecyclePort.onOpened` (U14 cài) → job `GROUP_DOC_CREATE`: với mỗi nhóm của lớp (U12) tạo một `GroupDocument` cho publication đó và các mục chính `TEACHER` từ khung (BR-U14-01); tạo job tự nộp tại hạn cuối nhận bài.
2. Nhóm tạo sau khi bài đã mở: U12 gọi `GroupChangePort.onGroupCreated` (U14 cài) → job `GROUP_DOC_CREATE` cho nhóm đó ở mọi publication bài nhóm đang mở của lớp.

## F2 - Mở tài liệu nhóm
1. Kiểm thành viên/giảng viên (BR-U14-03); trả tài liệu (phần chung + `publishedBlocks` + cây mục chính/mục chi tiết + trạng thái mục + người giữ + trạng thái tài liệu + bình luận chưa giải quyết).
2. Mở kênh realtime của tài liệu nhóm (`groupDocumentId`).

## F3 - Trưởng nhóm chia việc
1. Thêm mục chi tiết `GROUP` dưới một mục chính, sửa tiêu đề, đổi thứ tự hoặc xóa mục `GROUP` còn `OPEN` và rỗng (BR-U14-02).
2. Giao mục lá cho một thành viên (BR-U14-05): UPDATE có điều kiện → `CLAIMED`, `claimedBy` = thành viên, `assignedBy` = trưởng nhóm; nếu mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13).
3. Tài liệu đang `REVIEW` mà thêm mục hoặc giao lại mục → về `IN_PROGRESS` (BR-U14-25). Đẩy sự kiện tương ứng.

## F4 - Nhận mục
1. Thành viên nhận mục lá `OPEN` hoặc `DONE`: UPDATE có điều kiện → `CLAIMED`, `claimedBy`; `draftBlocks = publishedBlocks` (BR-U14-10). Tài liệu `REVIEW` → `IN_PROGRESS`.
2. Đẩy sự kiện `SECTION_CLAIMED`.

## F5 - Làm mục (trang riêng)
1. `DocumentEditor` chế độ `STUDENT` với block của mục; tự lưu `draftBlocks` (có version, như U11).
2. Chỉ người đang giữ mục được lưu.

## F6 - Xong mục và chuyển sang review
1. Kiểm (BR-U14-12) → cập nhật mục, `SectionRevision`, → `DONE`, bỏ khóa. Đẩy `SECTION_DONE` kèm nội dung mới.
2. Nếu mọi mục lá đều `DONE` → tài liệu `IN_PROGRESS` → `REVIEW` trong cùng transaction (BR-U14-24); đẩy `DOCUMENT_REVIEW`, báo các thành viên xem lại.

## F7 - Review, nhả khóa, bình luận
- Trong `REVIEW` mọi thành viên đọc tài liệu chung và bình luận trên từng mục; muốn sửa thì nhận lại hoặc trưởng nhóm giao lại mục (tài liệu về `IN_PROGRESS`).
- Nhả khóa, bình luận theo BR-U14-13, 14, 15; đẩy sự kiện tương ứng.

## F8 - Nộp
1. Trưởng nhóm nộp khi tài liệu ở `REVIEW` (BR-U14-30…32), hoặc job tự nộp theo hạn / khi ngưng giao (U08 gọi `PublicationLifecyclePort.onRetired`) (BR-U14-33).
2. Tạo `GroupSubmission` bất biến, biên nhận; trong cùng transaction gọi `GroupSubmittedPort` (U15 cài: tạo job chấm); sau commit phát `group.submitted` cho thông báo U16, đẩy `GROUP_SUBMITTED`.

## F9 - Cho U15
- `GroupSubmissionQueryPort` (BR-U14-40).
