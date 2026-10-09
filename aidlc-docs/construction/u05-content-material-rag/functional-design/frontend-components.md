# U05 Content, Material & RAG - Frontend Components

**Bản tài liệu 2026-10-09**: UC 15, 30, 33, 34, 36, 54, 55 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-CNT-001, US-CNT-002, US-CNT-004, US-CNT-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Material List | `MaterialListPage` | 54, 55 | Subject Manager | Manager Dashboard |
| Material Detail | `MaterialDetailPage` | 34, 55 | Subject Manager (học liệu của môn), Teacher hoặc Subject Manager được giao dạy (học liệu của lớp) | Material List, tab Materials của Teacher Class Detail |
| Tab Materials | `MaterialsTab` (gắn vào `TeacherClassPage` của U04) | 33, 34 | Teacher, Subject Manager được giao dạy | Teacher Class Detail |
| Learning Material | `LearningMaterialPage` | 15 | Student | Student Class Detail |
| Class Announcements | `ClassAnnouncementsPage` | 30, 36 | Student, Teacher, Subject Manager | Class Dashboard; Student Class Detail (lọc sẵn lớp) |

## 2. Cây component

```
app/manager/materials/                           MaterialListPage        màn Material List
  SubjectPicker                                  chọn môn mình quản lý
  ModuleList (chế độ môn)                        thêm, đổi tên, lên/xuống, lưu trữ module; học liệu của môn
app/manager/materials/[lessonId]/                MaterialDetailPage      màn Material Detail, học liệu của môn
app/classes/[id]/teaching/  MaterialsTab         tab Materials, ModuleList chế độ lớp
app/classes/[id]/teaching/materials/[lessonId]/  MaterialDetailPage      màn Material Detail, mở từ lớp
app/classes/[id]/materials/[lessonId]/           LearningMaterialPage    màn Learning Material
  LessonQuizList                                 chỗ gắn component của U11: quiz của học liệu, nút "Làm quiz"
app/classes/announcements/                       ClassAnnouncementsPage  màn Class Announcements
  ClassFilter                                    lọc theo lớp mình học hoặc dạy
  AnnouncementFeed                               thông báo mới nhất trước, phân trang
    AnnouncementCard                             lớp, tác giả, thời gian, nội dung; nút Sửa, Xóa cho giảng viên lớp
  AnnouncementForm                               tạo (chọn lớp) hoặc sửa thông báo
  DeleteAnnouncementDialog                       xác nhận xóa
shared/content/
  ModuleList                                     danh sách module của môn (chế độ môn hoặc chế độ lớp)
    ModuleItem                                   tiêu đề module, nút "Tải tệp", "Gắn link video", danh sách lesson
      LessonRow                                  tên, nhãn "Của môn"/"Của lớp", ScanStatusBadge, lên/xuống; bấm mở Material Detail
  ScanStatusBadge                                Chờ trích chữ / Đang trích chữ / Sẵn sàng tóm tắt (EXTRACTED) / Đang tóm tắt / Đã lập chỉ mục / Không có chữ / Không có phụ đề / Hệ thống đang bận, sẽ tự thử lại / Quét lỗi
  UploadLearningMaterialsDialog                  form thêm học liệu, mở từ nút của một module
    MaterialFileInput                            FileUploader (U03, purpose MATERIAL), nhiều tệp
    YoutubeUrlInput                              ô URL một video
  MaterialViewer                                 PDF xem trực tiếp và nút Tải (FileView), iframe youtube-nocookie (YoutubeView)
  SummaryAction                                  nút Tóm tắt tài liệu/trạng thái
  SummaryBlock                                   "Tóm tắt do AI tạo"
  MaterialEditForm                               sửa thông tin học liệu (tên), không đổi tệp hay link
  DeleteMaterialDialog                           xác nhận xóa (lưu trữ)
```

Route theo `RoleGuard` của U01: `/manager/*` cho Subject Manager; `/classes/*` cho Student, Teacher, Subject Manager. Backend vẫn kiểm quan hệ thật (Chủ nhiệm môn, giảng viên chính, ghi danh).

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `MaterialListPage`, `ModuleList` (chế độ môn) | Chọn môn; thêm, đổi tên, đổi thứ tự, lưu trữ module; xem học liệu của môn kèm trạng thái quét; lọc `ACTIVE`/`ARCHIVED` (UC 54, 55) | `GET /api/v1/subjects/{subjectId}/modules`, `POST /api/v1/subjects/{subjectId}/modules`, `PATCH /api/v1/modules/{id}` |
| `MaterialsTab`, `ModuleList` (chế độ lớp) | Module chỉ đọc; học liệu của môn chỉ đọc, học liệu của lớp quản lý được (UC 33) | `GET /api/v1/classes/{classId}/modules` |
| `LessonRow` | Đổi thứ tự theo quyền; bấm mở Material Detail | `PATCH /api/v1/lessons/{id}` |
| `UploadLearningMaterialsDialog` | Tải nhiều tệp hoặc một link vào module; upload/trích chữ không AI và không kiểm credit. Ghi chú: bấm Tóm tắt tài liệu trên màn xem khi cần | POST lesson của môn/lớp; U03 upload purpose MATERIAL |
| `MaterialDetailPage` | Thông tin, nguồn, người tải, trạng thái; SummaryAction/SummaryBlock theo quyền xem (POST summary, GET summary-credit/scan); xem và tải tệp; người quản lý học liệu có Sửa (đổi thông tin) và Xóa, không có Quét lại hay thay tài liệu; Teacher xem học liệu của môn chỉ đọc nội dung nhưng được bấm Tóm tắt tài liệu (UC 34, 55) | `GET /api/v1/lessons/{id}?classId=`, `POST /api/v1/lessons/{id}/download?classId=` |
| `MaterialEditForm`, `DeleteMaterialDialog` | Sửa tên, không đổi tệp hay link; xóa là lưu trữ, cảnh báo quiz của học liệu sẽ không còn hiện cho người học | `PATCH /api/v1/lessons/{id}` |
| `ScanStatusBadge` | Poll 3 giây khi `PENDING`/`SCANNING`/`BUSY` (`usePollStatus` của U03); không có nút Quét lại, `BUSY` báo hệ thống sẽ tự thử lại | `GET /api/v1/lessons/{id}/scan` |
| `LearningMaterialPage` | Student xem/tải học liệu; SummaryAction và SummaryBlock: nút Tóm tắt tài liệu, trạng thái và kết quả dùng chung; LessonQuizList của U11 | GET lesson/scan/summary-credit; POST summary với classId, Idempotency-Key; quiz qua U11 |
| `ClassAnnouncementsPage`, `ClassFilter`, `AnnouncementFeed` | Feed thông báo của các lớp `OPEN` mình học hoặc dạy, lọc theo lớp, mới nhất trước (UC 30) | `GET /api/v1/me/announcements?classId=` |
| `AnnouncementForm` | Giảng viên tạo thông báo (chọn lớp mình dạy) hoặc sửa kèm `version`; `409` yêu cầu tải lại (UC 36) | `POST /api/v1/classes/{classId}/announcements`, `PATCH /api/v1/announcements/{id}` |
| `DeleteAnnouncementDialog` | Xác nhận rồi xóa mềm kèm `version`; bỏ khỏi feed sau khi thành công (UC 36) | `DELETE /api/v1/announcements/{id}` |

Danh sách lớp để chọn khi tạo thông báo lấy từ API lớp của U04 (`GET /api/v1/classes?teacher=me`, lớp `OPEN`). Không có bình luận dưới thông báo (bỏ 2026-10-09). Tạo mới gửi thông báo trong app một lần; sửa hoặc xóa không gửi lại.

## SummaryAction trên View Material

Student, Teacher và Subject Manager có quyền xem đều có nút. EXTRACTED chưa yêu cầu: hiển thị mức giữ và số dư chính mình, bấm gửi Idempotency-Key; thiếu credit hiện mua thêm rồi bấm lại, không chặn xem/tải. Trích chữ/PENDING/SCANNING/BUSY: khóa nút và poll; đã có summary: hiện kết quả dùng chung, không tạo lại. NO_TEXT/NO_CAPTION/FAILED: báo trạng thái, không có retry thủ công. Teacher xem học liệu môn vẫn dùng nút dù không được sửa. Server kiểm quyền mọi lần; 404 ngoài scope, 403 Admin, 409 chưa sẵn sàng, lỗi credit/AI guard trước nhận giữ EXTRACTED. Không hiện chi phí hay số dư payer cho người xem khác.
