# U13 Reference Materials — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U13 Reference Materials thuộc Iter 3, người code: Người 4; UC 58–62. Lấy phần học liệu của U05 cũ; bỏ module, Lesson, tóm tắt, embedding và RAG (phần mở rộng US-CNT-005). Thông báo lớp của U05 cũ thuộc U14. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.
> Cập nhật 2026-10-17 theo design G21 (bản 17/10, 123 artboard): màn, popup, trường và thao tác lấy theo design; chỗ design khác quy tắc nghiệp vụ thì giữ quy tắc. Chỉ sửa tài liệu.

**Primary UC:** 58, 59, 60, 61, 62. **Primary stories:** US-CNT-001, US-CNT-002; US-CNT-005 là phần mở rộng. **Requirements:** FR-004, FR-005, FR-013.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

Design: [Nền tảng học tập – 45 màn theo screen flow G21](https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4), bản 17/10/2026; `#n` là số màn, mã trong ngoặc (ví dụ `a02`) là artboard.

## 1. Màn hình theo design

| # | Màn (artboard) | Component | UC | Role |
|---|---|---|---|---|
| 23 | Material List (`m13`), menu Thêm học liệu: Tải tệp / Link YouTube (`m13b`) | `SubjectMaterialListPage`, `AddFileDialog`, `AddYoutubeLinkDialog` | 58, 59 | Subject Manager |
| 24 | Material Detail (`m02`), popup Lưu trữ học liệu (`m02b`) | `MaterialDetailPage`, `ArchiveMaterialDialog` | 58–61 | Người quản lý học liệu; giảng viên xem học liệu của môn |
| 32 | Teacher Class Detail tab Học liệu (`t20`) | `ClassMaterialsTab` | 60, 61 | Teacher, Subject Manager được giao dạy |
| 36 | Student Class Detail, phần Học liệu (`s02`) | `ClassMaterialsCard` | 62 | Student |
| 37 | Learning Material: học liệu dạng tệp (`s05`), video YouTube (`s05b`) | `LearningMaterialPage` | 62 | Người có quyền xem |
| 17, 18 | Số học liệu trên Manager Dashboard, My Subject (`m11`, `m01`) | `MaterialSummary` | 58 | Subject Manager |

U13 đăng ký `NavEntry` "Học liệu" (Subject Manager). Học liệu không có module: là một danh sách có thứ tự.

## 2. Cây component

```
app/manager/materials/            SubjectMaterialListPage   #23 Material List
  MaterialTable                                             kéo để sắp thứ tự
  AddMaterialMenu                                           Tải tệp / Link YouTube
  AddFileDialog, AddYoutubeLinkDialog
app/materials/[id]/               MaterialDetailPage        #24 Material Detail
  MaterialPreview                                           PDF có chuyển trang, video nhúng
  MaterialInfoForm, ReplaceVersionDialog, VersionHistory
  ArchiveMaterialDialog
(TeacherClassTab "materials" của U05)
  ClassMaterialsTab                                         học liệu của lớp, học liệu của môn chỉ xem
(StudentClassSection của U05)
  ClassMaterialsCard
(SubjectSummarySection của U05)
  MaterialSummary                                           số học liệu đang dùng, lưu trữ
(slot AssignmentMaterialSection của U08, QuizMaterialSection của U11)
  MaterialLinkList                                          học liệu gắn kèm bài tập, Quiz
app/materials/[id]/view/          LearningMaterialPage      #37 Learning Material
  ClassMaterialNav                                          danh sách học liệu của lớp, mục đang xem
  PdfViewer, FileDownloadButton, YoutubeEmbed
  (vùng LearningMaterialSection cho U11)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `SubjectMaterialListPage`, `MaterialTable` | `GET /api/v1/subjects/{subjectId}/materials`, `PUT .../materials:reorder` | "Học liệu", "Học liệu của môn là một danh sách có thứ tự; sinh viên thấy đúng thứ tự này."; nút chọn môn, Thêm học liệu; tab Đang dùng, Lưu trữ (kèm số); "Kéo ở cột đầu tiên để sắp thứ tự"; cột kéo, #, Học liệu (tên; loại · dung lượng, video ghi "YouTube"), Trích chữ (Đang xử lý, Đã trích, Không có chữ, Lỗi; video "—"), Trạng thái; bấm dòng mở Material Detail; ghi chú "Chỉ tệp có trạng thái trích chữ. Link YouTube chỉ lưu đường dẫn video." |
| `AddMaterialMenu`, `AddFileDialog`, `AddYoutubeLinkDialog` | `FileUploader` (U03), `POST .../materials` | Menu: Tải tệp (loại và dung lượng theo cài đặt hiện hành của U03, ví dụ "PDF, DOCX, PPTX · tối đa 50 MB"), Link YouTube ("Một video giáo dục · dán đường dẫn"). Popup: Tên học liệu*, Mô tả (tùy chọn), tệp qua `FileUploader` hoặc đường dẫn YouTube (kiểm dạng link một video) |
| `MaterialDetailPage`, `MaterialPreview` | `GET /api/v1/materials/{id}` | Nút quay lại; tên kèm nhãn trạng thái; đường dẫn "Học liệu › mã môn"; nút Lưu trữ, Lưu thay đổi; "Xem trước": PDF xem ngay có chuyển trang, video nhúng |
| `MaterialInfoForm`, `ReplaceVersionDialog` | `PATCH /api/v1/materials/{id}`, `POST .../versions` | "Thông tin": Tên học liệu*, Mô tả (tùy chọn), Tệp hiện hành (tên, dung lượng · phiên bản) kèm nút Thay tệp ("Thay tệp tạo phiên bản mới; phiên bản cũ vẫn xem được."), Trích chữ; `409` báo tải lại |
| `VersionHistory` | `GET /api/v1/materials/{id}` | "Lịch sử phiên bản": Phiên bản, Người tạo, Thời điểm, Trích chữ, nhãn Hiện hành |
| `ArchiveMaterialDialog` | `POST .../archive` | "Lưu trữ học liệu?": "X sẽ ẩn với sinh viên và giảng viên. Học liệu không bị xóa; bạn có thể mở lại ở tab Lưu trữ."; "Bài tập, Quiz đang gắn học liệu này vẫn giữ liên kết nhưng sinh viên không mở được học liệu đã lưu trữ."; Hủy, Lưu trữ |
| `ClassMaterialsTab` | `GET /api/v1/classes/{classId}/materials`, `POST .../materials` | Lọc Tất cả, Của lớp (n), Của môn (n), Lưu trữ; nút Thêm học liệu; cột #, Học liệu, Phạm vi (Của lớp, Của môn), Trích chữ, thao tác Xem, Lưu trữ (học liệu của lớp) hoặc "Chỉ xem" (học liệu của môn); "Học liệu của lớp chỉ sinh viên lớp này thấy. Học liệu của môn do Chủ nhiệm môn quản lý, bạn chỉ xem." |
| `ClassMaterialsCard` | `GET /api/v1/me/classes/{classId}/materials` | "Học liệu" (số học liệu): danh sách có số thứ tự, tên, loại · dung lượng, nhãn "của lớp"; bấm mở Learning Material |
| `LearningMaterialPage`, `PdfViewer`, `YoutubeEmbed`, `FileDownloadButton` | `GET .../view`, `POST .../download-token` | Nút quay lại; tên; đường dẫn "Lớp của tôi › lớp › Học liệu"; nút Tải xuống (tệp) hoặc Mở trên YouTube (video); cột trái: danh sách học liệu của lớp, mục đang xem tô đậm; phần Quiz gắn với học liệu (U11); "Mô tả" kèm loại · dung lượng · ngày cập nhật; vùng xem: PDF xem ngay có chuyển trang, video qua `youtube-nocookie.com`. Không hiện thời lượng hay tên kênh YouTube (không lưu) |
| `MaterialLinkList` | - | "Học liệu gắn kèm": nhãn từng học liệu, bấm mở Learning Material |
| `MaterialSummary` | `GET /api/v1/subjects/{subjectId}/materials` (đếm) | Dòng tóm tắt trên thẻ môn (Manager Dashboard); thẻ số liệu "Học liệu" (n đang dùng · n lưu trữ) trên My Subject |

Route `/manager/materials` cho Subject Manager; `/materials/*` cho người có quyền (backend kiểm quyền thật).
