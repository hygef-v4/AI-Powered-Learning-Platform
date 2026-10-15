# U13 Reference Materials — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Material List | `SubjectMaterialListPage` | 58, 59 | Subject Manager |
| Material Detail | `MaterialDetailPage` | 58–61 | Người quản lý tài liệu; giảng viên xem tài liệu của môn |
| Teacher Class Detail, tab Materials | `ClassMaterialsTab` | 60, 61 | Teacher, Subject Manager được giao dạy |
| Student Class Detail (phần Học liệu) | `ClassMaterialsCard` | 62 | Student |
| Learning Material | `LearningMaterialPage` | 62 | Người có quyền xem |

## 2. Cây component

```
app/manager/materials/            SubjectMaterialListPage   Material List
  MaterialTable, AddFileDialog, AddYoutubeLinkDialog
app/materials/[id]/               MaterialDetailPage        Material Detail
  MaterialInfoForm, ReplaceVersionDialog, VersionHistory, ArchiveButton
(tab Materials, đăng ký qua TeacherClassTab của U05)
  ClassMaterialsTab                                         tài liệu của lớp, tài liệu của môn chỉ đọc
(Student Class Detail, đăng ký qua StudentClassSection của U05)
  ClassMaterialsCard
(slot AssignmentMaterialSection của U08, QuizMaterialSection của U11)
  MaterialLinkList                                          tài liệu gắn kèm Assignment, Quiz
app/materials/[id]/view/          LearningMaterialPage      Learning Material
  PdfViewer, FileDownloadButton, YoutubeEmbed
  (vùng LearningMaterialSection cho U11)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectMaterialListPage`, `ClassMaterialsTab` | `GET .../materials` | Sắp theo thứ tự; nhãn "Của môn" ở tab lớp |
| `AddFileDialog`, `AddYoutubeLinkDialog` | `FileUploader` (U03), `POST .../materials` | Kiểm loại, dung lượng theo giới hạn từ U03; kiểm dạng link một video |
| `MaterialInfoForm`, `ReplaceVersionDialog`, `ArchiveButton` | `PATCH`, `POST .../versions`, `POST .../archive` | `409` báo tải lại; lưu trữ có xác nhận |
| `VersionHistory` | `GET /api/v1/materials/{id}` | Phiên bản, người tạo, thời điểm, trạng thái chữ |
| `LearningMaterialPage`, `PdfViewer`, `YoutubeEmbed` | `GET .../view`, `POST .../download-token` | PDF xem ngay; video qua `youtube-nocookie.com` |

Route `/manager/materials` cho Subject Manager; `/materials/*` cho người có quyền (backend kiểm quyền thật).
