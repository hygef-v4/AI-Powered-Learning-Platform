# U12 Group Assignment — frontend-components

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role |
|---|---|---|---|
| Teacher Class Detail, tab Students (phần Nhóm) | `ClassGroupsPanel` | 57, 42 | Teacher, Subject Manager được giao dạy |
| Student Detail (popup, phần nhóm) | `StudentGroupInfo` | 57 | Như trên |
| Student Class Detail (My Group) | `MyGroupSection` | 54, 55 | Student |
| Assignment Form (phần Nhóm) | `GroupAssignmentPanel` | 53 | Teacher, Subject Manager được giao dạy |
| Assignment Detail (nút bài nhóm) | `OpenGroupWorkButton` | 56 | Student |
| Assignment Workspace (chế độ nhóm) | `GroupWorkspacePage` | 56 | Student; giảng viên chỉ xem |

## 2. Cây component

```
(tab Students, đăng ký qua TeacherClassTab của U05)
  ClassGroupsPanel
    GroupEditor                              tạo, thêm, chuyển, gỡ, chọn trưởng nhóm; lưu cả bộ
    RandomSplitDialog                        sĩ số tối đa, bản xem trước
    LeaderRequestList                        duyệt, từ chối
    GroupProgressTable                       tiến độ theo bài nhóm, nhả khóa mục
(popup Student Detail, StudentDetailSection của U05)  StudentGroupInfo
(Student Class Detail, StudentClassSection của U05)
  MyGroupSection                             nhóm, thành viên, tài liệu nhóm, yêu cầu gần nhất
    LeaderChangeRequestDialog
(Assignment Form, AssignmentFormSection của U07)  GroupAssignmentPanel
(Assignment Detail, GroupWorkEntry của U08)       OpenGroupWorkButton
app/group-docs/[id]/              GroupWorkspacePage   Assignment Workspace chế độ nhóm
  GroupDocumentView                          DocumentViewer (U07), toàn bộ tài liệu
  SectionList                                trạng thái, người giữ, Nhận, Giao, Nhả, Làm
  SectionEditorDialog                        popup che kín trang, hiện theo nhánh, DocumentEditor (U07)
  AssignSectionDialog, SubmitGroupDialog
  GroupSubmissionTab                         bản nộp, biên nhận, tác giả từng mục, DOCX
  useGroupDocStream                          SSE, tải lại khi kết nối lại
  (useActivityTracker của U08 khi bật theo dõi)
```

## 3. Component và API

| Component | API | Hành vi |
|---|---|---|
| `ClassGroupsPanel`, `GroupEditor`, `RandomSplitDialog` | `GET`, `PUT .../groups`, `POST .../groups:random-preview` | Sửa trên trình duyệt, lưu cả bộ; lỗi hiện theo nhóm |
| `LeaderRequestList` | `.../leader-requests`, `:approve`, `:reject` | Từ chối bắt buộc lý do |
| `GroupProgressTable` | `GET .../group-assignments/{assignmentId}/progress`, `:release` | Nhả khóa có cảnh báo |
| `MyGroupSection`, `LeaderChangeRequestDialog` | `GET /api/v1/me/classes/{classId}/group`, `POST .../leader-requests`, `:cancel` | Chỉ nhóm của mình |
| `GroupAssignmentPanel` | `GET .../groups` | Hiện lỗi, cảnh báo sẵn sàng |
| `GroupWorkspacePage`, `SectionList` | `GET /api/v1/group-docs/{id}`, `:claim`, `:assign`, `:release` | Nút theo quyền; cập nhật từ SSE |
| `SectionEditorDialog` | `PUT .../draft`, `:done` | Tự lưu 10 giây; `409` báo tải lại; Xong đóng popup |
| `SubmitGroupDialog`, `GroupSubmissionTab` | `POST .../submit`, `GET .../export.docx` | Chỉ trưởng nhóm thấy nút Nộp; báo mục chưa Xong |
| `useGroupDocStream` | `GET .../events` | Khi tự nộp: khóa trang "Đang nộp…" rồi chuyển sang thẻ Bản nộp |

Route `/group-docs/*` cho Student và giảng viên của lớp (backend kiểm quyền thật).
