# U05 Content, Material & RAG - Frontend Components

```
app/teaching/subjects/[id]/content/     SubjectContentPage   (Chủ nhiệm môn)
app/teaching/classes/[id]/content/      ClassContentPage     (giảng viên)
  ChapterList
    ChapterRow            tiêu đề, lên/xuống, lưu trữ
    LessonRow             tiêu đề, trạng thái bản, lên/xuống, lưu trữ
  LessonEditor
    VersionBar            "Đang phát hành: bản N" / "Bản nháp", nút Phát hành
    ItemList
      TextItemEditor      markdown + xem trước
      FileItemEditor      FileUploader (U03, purpose MATERIAL)
      YoutubeItemEditor   ô URL, danh sách video
    IngestionStatusBadge  Chờ / Đang xử lý / Đã lập chỉ mục / Không có chữ / Không có phụ đề / Lỗi + Thử lại
  SubjectLessonPicker     (chỉ trang lớp) chọn bài cấp môn đưa vào lớp
shared/content/
  LessonViewer            dùng trong LearnerClassPage của U04
    TextItemView          markdown đã làm sạch
    FileItemView          nút Tải (PDF: thêm nút Xem)
    YoutubeItemView       iframe youtube-nocookie
```

| Component | Hành vi | API |
|---|---|---|
| `ChapterList` | Cây chương/bài, nút lên/xuống thay kéo thả | `GET /api/v1/content/{scopeType}/{scopeId}/chapters` |
| `LessonEditor` | Mở bài: có `DRAFT` thì sửa `DRAFT`, không thì nút "Sửa" tạo bản nháp | `GET`, `POST /api/v1/lessons/{id}/draft` |
| `VersionBar` | Phát hành có hộp xác nhận | `POST /api/v1/lessons/{id}/publish` |
| `ItemList` | Thêm/sửa/xóa/đổi thứ tự mục trong bản nháp | `POST`, `PATCH`, `DELETE /api/v1/lesson-drafts/{id}/items` |
| `YoutubeItemEditor` | Kiểm URL phía client; sau khi lưu hiện từng video và trạng thái | như trên |
| `IngestionStatusBadge` | Poll 5 giây khi đang `PENDING`/`PROCESSING`; nút Thử lại khi `FAILED` | `POST /api/v1/source-documents/{id}/retry` |
| `SubjectLessonPicker` | Danh sách bài cấp môn đã phát hành, chọn chương đích | `POST`, `DELETE /api/v1/classes/{id}/lesson-links` |
| `FileItemView` | Gọi API lấy URL tải (token 5 phút) khi bấm | `POST /api/v1/classes/{classId}/items/{itemId}/download` |
