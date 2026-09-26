# U14 Group Document & Submission - Business Logic Model

## F1 - Khởi tạo tài liệu nhóm
1. U08 mở bài `GROUP` gọi `PublicationLifecyclePort.onOpened` (U14 cài) → job `GROUP_DOC_CREATE`: với mỗi nhóm (U12) tạo tài liệu nhóm và `Section` từ khung (BR-U14-01); tạo job tự nộp tại hạn cuối nhận bài.
2. Nhóm thêm sau khi mở: U12 gọi `GroupChangePort.onGroupCreated` (U14 cài) → job `GROUP_DOC_CREATE` cho nhóm đó.

## F2 - Mở tài liệu nhóm
1. Kiểm thành viên/giảng viên (BR-U14-03); trả tài liệu (phần chung + `publishedBlocks` + trạng thái mục + người nhận + bình luận chưa giải quyết).
2. Mở kênh realtime của tài liệu nhóm (`groupId`).

## F3 - Nhận mục
1. UPDATE có điều kiện `status IN (OPEN, IN_REVIEW)` → `CLAIMED`, `claimedBy`; `draftBlocks = publishedBlocks` (BR-U14-10).
2. Đẩy sự kiện `SECTION_CLAIMED`.

## F4 - Làm mục (trang riêng)
1. `DocumentEditor` chế độ `LEARNER` với block của mục; tự lưu `draftBlocks` (có version, như U11).
2. Chỉ người đang nhận được lưu.

## F5 - Xong mục
1. Kiểm (BR-U14-12) → cập nhật mục, `SectionRevision`, → `IN_REVIEW`, bỏ khóa.
2. Đẩy `SECTION_DONE` kèm nội dung mới.

## F6 - Nhả khóa, mục nhóm, bình luận
- Theo BR-U14-02, 13, 14, 15; đẩy sự kiện tương ứng.

## F7 - Nộp
1. Trưởng nhóm nộp (BR-U14-30…32) hoặc job tự nộp theo hạn / khi ngưng giao (U08 gọi `PublicationLifecyclePort.onRetired`) (BR-U14-33).
2. Tạo `GroupSubmission` bất biến, biên nhận; trong cùng transaction gọi `GroupSubmittedPort` (U15 cài: tạo job chấm); sau commit phát `group.submitted` cho thông báo U16, đẩy `GROUP_SUBMITTED`.

## F8 - Cho U15
- `GroupSubmissionQueryPort` (BR-U14-40).
