# U08 Assignment Work — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U08 Assignment Work thuộc Iter 2, người code: Người 3; UC 37–41. Lấy phần Assignment cá nhân, phản hồi Practice và dữ liệu hoạt động của U11 cũ. Lượt Quiz thuộc U11 mới, bài nhóm thuộc U12. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 37, 38, 39, 40, 41. **Primary stories:** US-ASM-003 (phần Assignment cá nhân), US-ASM-012; dữ liệu hoạt động của US-INT-001. **Requirements:** FR-007, FR-008, FR-018, FR-030, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Student Assignments | `StudentAssignmentsPage` | 37 | Student |
| Student Class Detail (phần Bài tập) | `ClassAssignmentsCard` | 37 | Student |
| Assignment Detail | `StudentAssignmentDetailPage` | 38 | Student |
| Assignment Workspace (bài cá nhân) | `AssignmentWorkspacePage` | 39 | Student |
| Submission History | `SubmissionHistoryPage` | 40, 41 | Student |

Chế độ nhóm của Assignment Workspace (UC 56) do U12 làm, dùng lại `DocumentEditor` của U07 và `useActivityTracker` của U08.

## 2. Cây component

```
(Student Class Detail, đăng ký qua StudentClassSection của U05)
  ClassAssignmentsCard                       bài sắp hết hạn của lớp, lối vào danh sách đã lọc
app/my-assignments/                 StudentAssignmentsPage       Student Assignments
  AssignmentFilters, StudentAssignmentTable
  (vùng StudentAssignmentResult cho U09)
app/my-assignments/[assignmentId]/  StudentAssignmentDetailPage  Assignment Detail
  InstructionsView, DocumentViewer (U07), RubricViewer (U07)
  ActivityNotice                              thông báo theo dõi khi bật
  AttemptSummary, StartAttemptButton
  (vùng GroupWorkEntry cho U12)
  (vùng AssignmentMaterialSection cho U13)
app/attempts/[attemptId]/           AssignmentWorkspacePage      Assignment Workspace
  DocumentEditor (U07, chế độ người học), AutosaveStatus
  DocxImportDialog, SubmitDialog, ReceiptView
app/my-assignments/[assignmentId]/history/  SubmissionHistoryPage  Submission History
  AttemptList, AttemptViewer, ReceiptView, DocxDownloadButton
  PracticeFeedbackPanel                       lịch sử phản hồi, AiSourcePicker (U06, chỉ nguồn cá nhân), AiTaskStatus (U06)
  (vùng SubmissionResultSection cho U09)
shared/activity/
  useActivityTracker                          dùng chung U08, U11, U12
  ActivityTimeline                            U09 dùng trong UC 42
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `StudentAssignmentsPage`, `ClassAssignmentsCard` | `GET /api/v1/me/assignments` | Lọc lớp, trạng thái; sắp theo hạn |
| `StudentAssignmentDetailPage` | `GET /api/v1/me/assignments/{assignmentId}` | Nút Bắt đầu, Tiếp tục theo điều kiện; thông báo theo dõi trước khi bắt đầu |
| `StartAttemptButton` | `POST /api/v1/assignments/{assignmentId}/attempts` | Trả lượt đang làm nếu đã có |
| `AssignmentWorkspacePage`, `AutosaveStatus` | `GET /api/v1/attempts/{attemptId}`, `PUT .../content` | Lưu 10 giây sau lần sửa cuối và khi rời trang; `409` báo tải lại, giữ bản đang sửa trong trình duyệt |
| `DocxImportDialog` | `POST .../docx:preview` | Xem trước rồi thêm vào cuối |
| `SubmitDialog`, `ReceiptView` | `POST .../submit` | Hiện lỗi `validateForSubmit` theo phần; hiện biên nhận |
| `SubmissionHistoryPage`, `AttemptViewer`, `DocxDownloadButton` | `GET /api/v1/assignments/{assignmentId}/attempts/mine`, `GET /api/v1/attempts/{attemptId}`, `GET .../export.docx` | Đánh dấu lượt được chấm; nút ‹ › chuyển lượt |
| `PracticeFeedbackPanel` | `POST`, `GET .../feedback` | Chỉ hiện với bài `PRACTICE`; nhãn "AI ước tính, không phải điểm chính thức" |
| `useActivityTracker` | `POST .../activity` (hoặc API của U11, U12) | Dùng trạng thái hiển thị của trang, không dùng blur; heartbeat 60 giây; gom lô, gửi lại khi lỗi |

Route `/my-assignments/*` và `/attempts/*` chỉ cho Student (dùng `RoleGuard` của U01). Backend vẫn kiểm quyền thật.
