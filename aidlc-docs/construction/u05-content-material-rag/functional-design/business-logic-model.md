# U05 Content, Material & RAG - Business Logic Model

## F1 - Quản lý chương và bài
1. Kiểm quyền theo phạm vi (BR-U05-01, 02).
2. Tạo/sửa tiêu đề/đổi thứ tự/lưu trữ chương; tạo bài kèm bản `DRAFT` số 1.

## F2 - Soạn bản nháp
1. Nếu bài chỉ có bản `PUBLISHED` → tạo `DRAFT` sao chép mục (BR-U05-11).
2. Thêm/sửa/xóa/đổi thứ tự mục trong `DRAFT`:
   - `TEXT`: lưu markdown, tính `contentKey`, tạo hoặc dùng lại `SourceDocument`.
   - `FILE`: frontend upload qua U03 (`MATERIAL`) → `attach` vào bài → tạo hoặc dùng lại `SourceDocument`.
   - `YOUTUBE`: kiểm URL (BR-U05-22), tạo `YoutubeSource`, tạo job `U05_YOUTUBE_RESOLVE`.
3. `SourceDocument` mới ở `PENDING` → tạo job `U05_INGEST` (BR-U05-31).

## F3 - Phát hành
1. Kiểm quyền, `DRAFT` có ≥ 1 mục (BR-U05-13).
2. `DRAFT` → `PUBLISHED`, bản cũ → `SUPERSEDED`; audit (BR-U05-12).

## F4 - Liên kết bài cấp môn vào lớp
1. Người quản lý lớp chọn bài cấp môn có bản `PUBLISHED` → tạo `ClassLessonLink` trong chương của lớp; audit.
2. Gỡ liên kết → xóa link; audit.

## F5 - Job `U05_YOUTUBE_RESOLVE` (worker)
1. `VIDEO` → một `YoutubeVideo`; `PLAYLIST` → gọi YouTube Data API lấy ≤ 50 video.
2. Mỗi video tạo hoặc dùng lại `SourceDocument` (`contentKey = videoId`) → job `U05_INGEST`.
3. URL không tồn tại/riêng tư → `YoutubeSource.FAILED`.

## F6 - Job `U05_INGEST` (worker)
1. `SourceDocument` → `PROCESSING`.
2. Lấy chữ: `TEXT` từ markdown; `FILE` mở qua U03 và trích chữ theo trang; `YOUTUBE_VIDEO` lấy caption (BR-U05-33).
3. Không có chữ → `NO_TEXT`; không caption → `NO_CAPTION`; kết thúc.
4. Cắt đoạn (BR-U05-34), gọi `EmbeddingPort` theo lô ≤ 100 đoạn.
5. Một transaction: xóa đoạn cũ, ghi đoạn mới, `INDEXED`, `chunkCount` (BR-U05-36).
6. Lỗi: tạm → ném lỗi để U02 retry; vĩnh viễn hoặc hết lượt retry → `FAILED` với `errorCode` (BR-U05-37).

## F7 - Retry thủ công
1. Người quản lý bấm "Thử lại" trên tài liệu `FAILED` → `PENDING`, tạo job mới; audit.

## F8 - Học viên xem và tải
1. U04 gọi `PublishedContentPort.listForClass(classId)` sau khi đã kiểm ghi danh.
2. Học viên bấm tải file → U05 kiểm `ClassAccessPort.isActiveLearner`, lớp `OPEN`, mục thuộc bản `PUBLISHED` hiển thị trong lớp → `issueDownloadToken` (BR-U05-23).

## F9 - `retrieve(scope, query, k)`
1. Kiểm phạm vi (BR-U05-40), lấy danh sách `SourceDocument` của bài `PUBLISHED` trong phạm vi (BR-U05-41).
2. Tạo vector câu hỏi qua `EmbeddingPort`.
3. Tìm `k` đoạn gần nhất (cosine) trong các tài liệu đó; trả kèm nguồn (BR-U05-42).
