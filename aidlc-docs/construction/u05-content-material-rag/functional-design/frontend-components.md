# U05 Content, Material & RAG - Frontend Components

Màn hình theo [screen flow](../../../../docs/screen-flow.md): học liệu không có trang riêng. Chủ nhiệm môn quản lý module và học liệu của môn trên Subject Detail; Class Detail hiện cùng bộ module (giảng viên tải học liệu của lớp, người học xem). Mỗi module có nút "Tải tệp" và "Gắn link video" mở popup Upload Learning Materials đã chọn sẵn module; người học xem ở popup View Learning Material. Thông báo và bình luận ở màn Announcements.

```
shared/content/
  ModuleList                 danh sách module của môn; gắn vào Subject Detail (U04, chế độ môn: Chủ nhiệm môn thêm, đổi tên, lên/xuống, lưu trữ module và tải học liệu của môn) và Class Detail (U04, chế độ lớp: module chỉ đọc, giảng viên tải học liệu của lớp, người học xem)
    ModuleItem               tiêu đề module, nút "Tải tệp", "Gắn link video" (Chủ nhiệm môn ở chế độ môn, giảng viên lớp ở chế độ lớp), danh sách lesson
      LessonRow              tên, nhãn "Của môn"/"Của lớp", ScanStatusBadge, đổi tên, lên/xuống, lưu trữ (theo quyền BR-U05-02); người học bấm mở View Learning Material
      ScanStatusBadge        Chờ / Đang quét / Đã lập chỉ mục / Không có chữ / Không có phụ đề / Không đủ credit AI / Hệ thống đang bận / Lỗi + Quét lại
  UploadLearningMaterialsDialog   popup Upload Learning Materials (UC 11), mở từ nút của một module
    MaterialFileInput        FileUploader (U03, purpose MATERIAL), nhiều tệp (chế độ "Tải tệp")
    YoutubeUrlInput          ô URL một video (chế độ "Gắn link video")
  ViewLearningMaterialDialog           popup View Learning Material của Student (UC 12)
    FileView                 PDF xem trực tiếp, nút Tải
    YoutubeView              iframe youtube-nocookie
app/classes/[id]/communication/   màn Announcements (UC 13, UC 14)
  AnnouncementFeed           thông báo lớp, phân trang, mới nhất trước
    AnnouncementCard         nội dung; 2 bình luận mới nhất, tổng số bình luận, nút "Xem thêm bình luận", ô viết bình luận
  AnnouncementForm           giảng viên đăng thông báo
  CommentsDialog             popup toàn bộ bình luận của một thông báo (phân trang, cũ → mới) kèm ô viết bình luận; giảng viên ẩn bình luận có lý do
```

| Component | Hành vi | API |
|---|---|---|
| `ModuleList` | Chế độ môn (Subject Detail): Chủ nhiệm môn thêm, đổi tên, đổi thứ tự, lưu trữ module, xem học liệu của môn. Chế độ lớp (Class Detail): module chỉ đọc, kèm học liệu của môn và của lớp | `GET /api/v1/subjects/{subjectId}/modules`, `POST /api/v1/subjects/{subjectId}/modules`, `PATCH /api/v1/modules/{id}`, `GET /api/v1/classes/{classId}/modules` |
| `LessonRow` | Sửa tên, đổi thứ tự, lưu trữ theo quyền; người học mở View Learning Material | `PATCH /api/v1/lessons/{id}` |
| `UploadLearningMaterialsDialog` | Mở từ nút của module; tải nhiều tệp hoặc một link video; đóng popup là xong, quét chạy nền | Học liệu của môn: `POST /api/v1/subjects/{subjectId}/modules/{moduleId}/lessons`; học liệu của lớp: `POST /api/v1/classes/{classId}/modules/{moduleId}/lessons` |
| `ScanStatusBadge` | Poll 3 giây khi `PENDING`/`SCANNING` (`usePollStatus` của U03); nút Quét lại khi `FAILED`, `BUSY`, `NO_CREDIT` | `GET /api/v1/lessons/{id}/scan`, `POST /api/v1/lessons/{id}/scan` |
| `ViewLearningMaterialDialog` | Lấy URL tải (token 5 phút) khi mở tệp | `POST /api/v1/classes/{classId}/lessons/{lessonId}/download` |
| `AnnouncementFeed`, `AnnouncementForm` | Thành viên lớp xem; giảng viên đăng hoặc ẩn có lý do; mỗi thông báo kèm 2 bình luận mới nhất | `GET`, `POST /api/v1/classes/{id}/announcements`, `POST /api/v1/classes/{id}/announcements/{announcementId}:hide` |
| `AnnouncementCard`, `CommentsDialog` | Viết bình luận (≤ 2 000 ký tự); "Xem thêm bình luận" mở popup toàn bộ; giảng viên ẩn bình luận có lý do | `GET`, `POST /api/v1/announcements/{id}/comments`, `POST /api/v1/announcement-comments/{id}:hide` |
