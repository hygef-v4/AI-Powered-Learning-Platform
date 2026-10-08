# U05 Content, Material & RAG - Frontend Components

**Bản tài liệu 2026-10-08**: UC 14, 26, 29, 30, 31, 51, 52; primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Màn hình theo [UC/màn hình hiện hành](../../../../docs/use-cases-and-screens.md): Uploaded Learning Materials là danh sách học liệu lớp cho actor R3/R4, mở từ Class Detail (UC 29–30). Subject Detail chứa học liệu/module của môn cho R2 (UC 51–52); Class Detail hiện cùng bộ module, module chỉ đọc trong phạm vi lớp. Danh sách dùng lại ModuleList/LessonRow. Nút "Tải tệp" hoặc "Gắn link video" mở form hỗ trợ đã chọn module; form này không phải màn Uploaded Learning Materials. Learning Material là màn xem học liệu theo quyền UC 14. Thông báo và bình luận ở Class Announcements.

```
shared/content/
  UploadedLearningMaterialsView  màn Uploaded Learning Materials (UC 29–30), danh sách trong phạm vi lớp, reuse ModuleList/LessonRow
  ModuleList                 danh sách module của môn; gắn vào Subject Detail (U04, chế độ môn: Chủ nhiệm môn thêm, đổi tên, lên/xuống, lưu trữ module và tải học liệu của môn) và Class Detail (U04, chế độ lớp: module chỉ đọc, giảng viên tải học liệu của lớp, người học xem)
    ModuleItem               tiêu đề module, nút "Tải tệp", "Gắn link video" (Chủ nhiệm môn ở chế độ môn, giảng viên lớp ở chế độ lớp), danh sách lesson
      LessonRow              tên, nhãn "Của môn"/"Của lớp", ScanStatusBadge, đổi tên, lên/xuống, lưu trữ (theo quyền BR-U05-02); người học bấm mở Learning Material
      ScanStatusBadge        Chờ / Đang quét / Đã lập chỉ mục / Không có chữ / Không có phụ đề / Không đủ credit AI / Hệ thống đang bận / Lỗi + Quét lại
  UploadLearningMaterialsDialog   form hỗ trợ thêm học liệu UC 30/52, mở từ nút của một module
    MaterialFileInput        FileUploader (U03, purpose MATERIAL), nhiều tệp (chế độ "Tải tệp")
    YoutubeUrlInput          ô URL một video (chế độ "Gắn link video")
  ViewLearningMaterialDialog           màn Learning Material theo R2/R3/R4/R5 (UC 14)
    FileView                 PDF xem trực tiếp, nút Tải
    YoutubeView              iframe youtube-nocookie
app/classes/[id]/communication/   màn Class Announcements (UC 26, 31)
  AnnouncementFeed           thông báo lớp, phân trang, mới nhất trước
    AnnouncementCard         nội dung; 2 bình luận mới nhất, tổng số bình luận, nút "Xem thêm bình luận", ô viết bình luận
  AnnouncementForm           giảng viên đăng thông báo
  CommentsDialog             popup toàn bộ bình luận của một thông báo (phân trang, cũ → mới) kèm ô viết bình luận; giảng viên ẩn bình luận có lý do
```

| Component | Hành vi | API |
|---|---|---|
| `ModuleList` | Chế độ môn (Subject Detail): Chủ nhiệm môn thêm, đổi tên, đổi thứ tự, lưu trữ module, xem học liệu của môn. Chế độ lớp (Class Detail): module chỉ đọc, kèm học liệu của môn và của lớp | `GET /api/v1/subjects/{subjectId}/modules`, `POST /api/v1/subjects/{subjectId}/modules`, `PATCH /api/v1/modules/{id}`, `GET /api/v1/classes/{classId}/modules` |
| `LessonRow` | Sửa tên, đổi thứ tự, lưu trữ theo quyền; người học mở Learning Material | `PATCH /api/v1/lessons/{id}` |
| `UploadLearningMaterialsDialog` | Mở từ nút của module; tải nhiều tệp hoặc một link video; đóng popup là xong, quét chạy nền | Học liệu của môn: `POST /api/v1/subjects/{subjectId}/modules/{moduleId}/lessons`; học liệu của lớp: `POST /api/v1/classes/{classId}/modules/{moduleId}/lessons` |
| `ScanStatusBadge` | Poll 3 giây khi `PENDING`/`SCANNING` (`usePollStatus` của U03); nút Quét lại khi `FAILED`, `BUSY`, `NO_CREDIT` | `GET /api/v1/lessons/{id}/scan`, `POST /api/v1/lessons/{id}/scan` |
| `ViewLearningMaterialDialog` | Xem học liệu được phép, lấy URL tải (token 5 phút) | `GET /api/v1/lessons/{id}`, `POST /api/v1/lessons/{id}/download` |
| `AnnouncementFeed`, `AnnouncementForm` | R5 hoặc R3/R4 xem; R3/R4 tạo/sửa/xóa; mỗi thông báo kèm 2 bình luận mới nhất | `GET`, `POST /api/v1/classes/{id}/announcements`; `PATCH`, `DELETE /api/v1/announcements/{id}` với version |
| `AnnouncementCard`, `CommentsDialog` | Viết bình luận (≤ 2 000 ký tự); "Xem thêm bình luận" mở popup toàn bộ; giảng viên ẩn bình luận có lý do | `GET`, `POST /api/v1/announcements/{id}/comments`, `POST /api/v1/announcement-comments/{id}:hide` |

## Update/Delete trên Class Announcements (UC 31)
AnnouncementForm dùng cho create/edit; PATCH /api/v1/announcements/{id} với version. DeleteAnnouncementAction xác nhận và DELETE cùng URL/version; bỏ khỏi feed sau thành công. Chỉ R3/R4 có nút; 409 yêu cầu tải lại. Không gửi notification tạo mới khi edit/delete. Comment giữ quy tắc chỉ tạo/ẩn vi phạm.
Learning Material dùng GET /api/v1/lessons/{id} và POST /api/v1/lessons/{id}/download theo actor R2/R3/R4/R5; endpoint lớp cũ có thể giữ alias nhưng không ép Teacher/Admin thành Student.
