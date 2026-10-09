# U14 Group Document & Submission - Frontend Components

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Group Essay Workspace | `GroupDocumentPage` | 27 | Student (thành viên nhóm) | Assignment Detail |
| Popup làm phần (che kín trang, trong Group Essay Workspace) | `SectionEditorDialog` | 27 | Student giữ mục | Group Essay Workspace |
| Assignment Detail (phần bài nhóm) | `GroupAssignmentPanel` gắn vào `AssignmentOverviewPage` của U11 | 23, 27 | Student (thành viên nhóm) | Student Assignments |
| Submission History (bài nhóm) | `GroupSubmissionView` gắn vào màn Submission History của U11 | 28 | Student (thành viên nhóm) | Assignment Detail; tự chuyển sau khi tự nộp |
| My Group (trong Student Class Detail) | `GroupDocsOfGroupList` gắn vào `MyGroupCard` của U12 | 16 | Student (thành viên nhóm) | Student Class Detail |
| Tab Evals, danh sách bài nộp của bài nhóm | `GroupDocsOverviewPanel` gắn vào màn bài nộp của U15 | 37 | Teacher, Subject Manager được giao dạy (giảng viên chính của lớp) | Teacher Class Detail |
| Submission Detail (bài nhóm) | `GroupSubmissionView` (chế độ giảng viên) dùng trong màn của U15 | 37 | Teacher, Subject Manager được giao dạy | Tab Evals |

Group Essay Workspace chỉ mở từ Assignment Detail (screen flow Page-2); My Group liệt kê tài liệu và mở Assignment Detail của bài. Admin không vào các màn này. Phần soạn bài nhóm (khung, chia phần, rubric từng phần trong popup Rubric Detail) trên Assignment Form thuộc U08/U09 (UC 45, 46); U14 chỉ dựng mỗi phần thành một mục khi bài mở.

## 2. Cây component

```
app/classes/[id]/group-docs/[groupDocumentId]/   GroupDocumentPage (màn Group Essay Workspace)
  GroupDocHeader            tên bài, tên nhóm, trưởng nhóm, hạn, đồng hồ đếm ngược, trạng thái nộp, nút Nộp (chỉ trưởng nhóm), Tải DOCX
  SectionOutline            danh sách mục (phần của khung): trạng thái (Trống / Đang làm bởi X / Xong), người giao, nút Nhận, Mở (người giữ), Nhả
  SharedBlocksView          phần chung của khung (chỉ đọc)
  SectionView               publishedBlocks (chỉ đọc); mục và phần chung không có nút sửa/xóa
  AssignSectionsDialog      (trưởng nhóm) giao từng mục cho thành viên
  SectionEditorDialog       popup che kín trang: heading trên nhánh của phần và nội dung của chúng + block của mục, không hiện nhánh khác
                            (DocumentEditor U09, mode STUDENT), "Đã lưu lúc …", nút Xong, nút Nhả
  ReleaseConfirmDialog      hộp xác nhận: bản nháp chưa Xong của người giữ sẽ bị bỏ
  SubmitGroupDialog         trưởng nhóm xác nhận nộp; cảnh báo các mục chưa xong
  AutoSubmitOverlay         khi tự nộp hoặc bài ngưng giao: khóa trang, người đang sửa gửi lần lưu cuối (khi hết hạn), vòng chờ "Đang nộp…";
                            nộp xong chuyển sang Submission History
  useGroupDocStream         kết nối SSE, áp dụng sự kiện, tải lại khi kết nối lại
shared/group-docs/GroupAssignmentPanel      trong Assignment Detail (U11) khi bài là bài nhóm: nhóm, trưởng nhóm, thành viên, tiến độ mục,
                                            trạng thái nộp, nút "Mở tài liệu nhóm"
shared/group-docs/GroupSubmissionView       bản nộp cuối (chỉ đọc, thời điểm, người nộp, cách nộp, trễ, biên nhận, tác giả từng mục, tải DOCX);
                                            trong Submission History (U11) và Submission Detail (U15)
shared/group-docs/GroupDocsOfGroupList      trong MyGroupCard (U12): mỗi bài nhóm một dòng (tiến độ, đã nộp), mở Assignment Detail
shared/group-docs/GroupDocsOverviewPanel    (giảng viên chính của lớp) trong danh sách bài nộp của bài nhóm (U15)
  GroupProgressTable        mỗi nhóm một dòng: số mục xong/đang làm/trống, đã nộp, trễ, cách nộp; mở rộng xem mục và người giữ
  ReleaseConfirmDialog      nhả khóa một mục
```

Route theo `RoleGuard` của U01 (`/classes/*`); backend vẫn kiểm R5, thành viên nhóm và R3/R4.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `GroupAssignmentPanel` | Chưa có nhóm thì báo "Bạn chưa thuộc nhóm nào của lớp"; chưa có tài liệu thì "Tài liệu nhóm đang được tạo"; nút Mở tài liệu nhóm → Group Essay Workspace (UC 23, 27) | `GET /api/v1/assignments/{assignmentId}/group-docs/mine` |
| `GroupDocsOfGroupList` | Chỉ nhóm của mình; bấm dòng mở Assignment Detail của bài (UC 16) | `GET /api/v1/groups/{groupId}/group-docs` |
| `GroupDocumentPage` | Tài liệu `CLOSED` hoặc bài hết nhận bài nộp thì chỉ đọc | `GET /api/v1/group-docs/{id}` |
| `useGroupDocStream` | SSE; mất kết nối → thử lại, tải lại toàn bộ | `GET /api/v1/group-docs/{id}/events` (SSE) |
| `SectionOutline` Nhận | `409` hiện "Mục vừa được X nhận" | `POST /api/v1/group-docs/{id}/sections/{sid}/claim` |
| `AssignSectionsDialog` | Chỉ trưởng nhóm; chọn thành viên đang hiệu lực; không thêm/xóa/sửa mục | `POST /api/v1/group-docs/{id}/sections/{sid}/assign` |
| `SectionEditorDialog` | Mở toàn màn hình; tự lưu 10 s; `409` lệch `version` báo tải lại; Xong đóng popup, workspace của mọi người cập nhật realtime | `PUT .../sections/{sid}/draft`, `POST .../done` |
| `ReleaseConfirmDialog` | Cảnh báo bỏ bản nháp chưa Xong rồi nhả | `POST /api/v1/group-docs/{id}/sections/{sid}/release` |
| `SubmitGroupDialog` | Chỉ trưởng nhóm, bất kỳ lúc nào khi bài còn nhận; liệt kê mục chưa xong; hiện biên nhận | `POST /api/v1/group-docs/{id}/submissions` |
| `AutoSubmitOverlay` | Kích hoạt khi đồng hồ về 0 hoặc nhận `ASSIGNMENT_RETIRED`; chờ `GROUP_SUBMITTED` (mất kết nối thì hỏi trạng thái) rồi chuyển sang Submission History | `GET /api/v1/group-docs/{id}/submission` |
| `GroupSubmissionView` | Bản nộp cuối; chưa nộp thì báo "Nhóm chưa nộp"; điểm/phản hồi đã công bố do U15 hiện (UC 28, 37) | `GET /api/v1/group-docs/{id}/submission` |
| Tải DOCX | Thành viên: bản hiện tại hoặc bản nộp; giảng viên: bản nộp | `GET /api/v1/group-docs/{id}/export.docx?submission=` |
| `GroupDocsOverviewPanel` | Tiến độ các nhóm; mở rộng dòng đọc mục và người giữ; nhả khóa; mở Submission Detail của nhóm đã nộp (UC 37) | `GET /api/v1/assignments/{assignmentId}/group-docs`, `GET /api/v1/group-docs/{id}`, `POST .../sections/{sid}/release` |
