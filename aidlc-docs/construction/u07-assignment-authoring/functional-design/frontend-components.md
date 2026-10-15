# U07 Assignment Authoring — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U07 Assignment Authoring thuộc Iter 2, người code: Người 4; UC 34–36. Lấy phần Assignment của U08 cũ (vòng đời, phát hành), U09 cũ (tài liệu, khung, DOCX) và rubric của U06 cũ. Quiz và ngân hàng thuộc U10. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 34, 35, 36. **Primary stories:** US-ASM-001 (phần Assignment), US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001. **Requirements:** FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

Nguồn: [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Teacher Class Detail, tab Evals (phần Bài tập) | `ClassAssignmentsSection` | 34, 35 | Teacher, Subject Manager được giao dạy |
| Assignment List | `SubjectAssignmentListPage` | 34, 35 | Subject Manager |
| Assignment Form | `AssignmentFormPage` | 35 | Người có quyền với bài; giảng viên lớp chỉ xem bài của môn |
| Rubric Detail (popup) | `RubricDetailDialog` | 36 | Như Assignment Form |

## 2. Cây component

```
(tab Evals, đăng ký qua TeacherClassTab của U05)
  ClassAssignmentsSection                     bài của lớp và của môn, lọc trạng thái
    CreateAssignmentDialog                    cá nhân hay nhóm, sơ đồ, chế độ, nguồn
    CopySourcePicker                          lớp mình dạy và bài để copy
    (vùng AssignmentRowAction cho U09)
app/manager/assignments/  SubjectAssignmentListPage   Assignment List
app/assignments/[id]/     AssignmentFormPage          Assignment Form
  AssignmentInfoPanel                         tiêu đề, hướng dẫn
  AssignmentSettingsPanel                     sơ đồ, requiredDiagrams, chế độ, theo dõi hoạt động, tài liệu gắn kèm
  SkeletonEditor                              dùng DocumentEditor ở chế độ người soạn
  PartList                                    phần, tổng điểm, cảnh báo rubric trống
    RubricDetailDialog                        popup Rubric Detail
  AiDraftPanel                                yêu cầu, tài liệu, AiSourcePicker (U06), AiTaskStatus (U06), xem trước đề xuất
  DocxImportDialog, DocxExportButton
  PreviewDialog                               xem như người học
  ReviewButton, PublishDialog                 duyệt; lịch, số lượt, nộp trễ, xác nhận nhóm
  ScheduleDialog, RetireDialog, CloneButton, NewVersionButton, DeleteButton
  (vùng AssignmentFormSection cho U12)
app/assignments/[id]/diagrams/[blockId]/   DiagramViewPage   trang xem sơ đồ từ link trong DOCX
shared/document/
  DocumentEditor                              trình soạn block dùng chung U07, U08, U12
  DiagramBlock                                ảnh xem trước, mở Draw.io nhúng
  DocumentViewer, RubricViewer
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassAssignmentsSection`, `SubjectAssignmentListPage` | `GET /api/v1/classes/{classId}/assignments`, `GET /api/v1/subjects/{subjectId}/assignments` | Bài của môn có nhãn "Của môn", chỉ đọc với giảng viên lớp; mặc định ẩn `CLOSED`, `RETIRED` |
| `CreateAssignmentDialog`, `CopySourcePicker` | `POST .../assignments`, `GET .../assignment-copy-sources`, `POST .../assignments:copy` | Ô nhóm chỉ hiện ở bài của lớp khi U12 có; nhóm khóa chế độ `GRADED` |
| `AssignmentInfoPanel`, `AssignmentSettingsPanel` | `PATCH /api/v1/assignments/{id}`, `GET .../selectable-materials` | Tắt sơ đồ bị từ chối thì hiện chỗ cần bỏ; chọn tài liệu gắn kèm khi U13 có |
| `SkeletonEditor` | `PUT .../skeleton` | Lưu tự động khi ngừng gõ 2 giây và khi rời trang; `409` báo tải lại, giữ bản đang sửa |
| `RubricDetailDialog` | `GET`, `PUT .../parts/{partId}/rubric` | Tổng điểm tự tính; bài đã phát hành chỉ xem |
| `AiDraftPanel` | `POST .../ai-drafts`, `GET .../ai-drafts/{draftId}`, `:apply`, `:discard` | Hiện provenance; Áp dụng có cảnh báo thay khung |
| `DocxImportDialog`, `DocxExportButton` | `POST .../skeleton:import-docx`, `GET .../docx` | Bản xem trước ghi sơ đồ khôi phục theo cách nào; chọn thay khung hoặc thêm vào cuối |
| `PublishDialog`, `ScheduleDialog`, `RetireDialog` | `/publish`, `PATCH .../schedule`, `/retire` | Giờ theo Asia/Ho_Chi_Minh; lỗi nhóm của U12 hiện trong dialog |
| `DiagramBlock` | - | Draw.io nhúng từ `embed.diagrams.net` qua iframe, kiểm origin của message; lưu XML và SVG |

Route `/assignments/*` và `/manager/assignments` cho Teacher, Subject Manager (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
