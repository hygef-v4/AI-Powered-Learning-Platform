# U14 Group Document & Submission - Frontend Components

**Bản tài liệu 2026-10-08**: UC 23; primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
app/learning/group-docs/[id]/                  GroupDocumentPage (màn Group Essay Workspace, mở từ Assignment Detail hoặc My Group)
  GroupDocHeader            tên nhóm, trưởng nhóm, hạn, đồng hồ đếm ngược, trạng thái nộp, nút Nộp (chỉ trưởng nhóm), Tải DOCX
  SectionOutline            danh sách mục (phần của khung): trạng thái (Trống / Đang làm bởi X / Xong), người giao, nút Nhận
  SharedBlocksView          phần chung của khung giảng viên (chỉ đọc)
  SectionView               publishedBlocks (chỉ đọc); mục và phần chung của giảng viên không có nút sửa/xóa
  AssignSectionsDialog      (trưởng nhóm) giao từng mục cho thành viên
  SectionEditorDialog       popup che kín trang khi người giữ mở mục: các heading trên nhánh của mục và nội dung của chúng + các block của mục, không hiện nhánh khác (DocumentEditor U09, mode STUDENT), "Đã lưu lúc …", nút Xong, nút Nhả (hộp xác nhận: bản nháp chưa Xong sẽ bị bỏ)
  ReleaseLockButton         (trưởng nhóm; giảng viên ở trang giảng viên); hộp xác nhận: bản nháp chưa Xong của người giữ sẽ bị bỏ
  SubmitGroupDialog         trưởng nhóm xác nhận nộp; cảnh báo các mục chưa xong
  AutoSubmitOverlay         khi tự nộp, với mọi người đang mở tài liệu (đang sửa mục hoặc chỉ xem): khóa trang, người đang sửa gửi lần lưu cuối, vòng chờ "Đang nộp…"; nộp xong chuyển sang Submission History
  useGroupDocStream         kết nối SSE, áp dụng sự kiện, tải lại khi kết nối lại
shared/group-docs/GroupSubmissionView       bản nộp của nhóm (chỉ đọc, tác giả từng mục, tải DOCX); hiện trong màn Submission History (U11) khi bài là bài nhóm
shared/group-docs/GroupDocsOverviewPanel    (giảng viên; hiện trong Student Submissions (U15) khi là bài nhóm, thay bảng theo từng sinh viên)
  bảng nhóm: số mục xong/đang làm/trống, bản nộp, xem tài liệu (mở chi tiết bài nộp của nhóm), nhả khóa
```

Phần soạn bài nhóm (khung, chia phần, rubric từng phần) trên Assignment Editor, Question Bank và Template Editor (UC 43) thuộc U09/U06; U14 chỉ dựng mỗi phần thành một mục khi bài mở trong lớp.

| Component | Hành vi | API |
|---|---|---|
| `GroupDocumentPage` | | `GET /api/v1/group-docs/{id}` |
| `useGroupDocStream` | SSE; mất kết nối → thử lại, tải lại toàn bộ | `GET /api/v1/group-docs/{id}/events` (SSE) |
| `SectionOutline` Nhận | | `POST /api/v1/group-docs/{id}/sections/{sid}/claim` |
| `AssignSectionsDialog` | Chỉ trưởng nhóm; không thêm/xóa/sửa mục | `POST /api/v1/group-docs/{id}/sections/{sid}/assign` |
| `SectionEditorDialog` | Mở toàn màn hình; tự lưu 10 s; Xong đóng popup, workspace của mọi người cập nhật realtime | `PUT .../sections/{sid}/draft`, `POST .../done`, `POST .../release` |
| `SubmitGroupDialog` | Chỉ trưởng nhóm, bất kỳ lúc nào khi bài còn nhận | `POST /api/v1/group-docs/{id}/submissions` |
| `AutoSubmitOverlay` | Chờ sự kiện `GROUP_SUBMITTED` (mất kết nối thì hỏi trạng thái) rồi chuyển trang | `GET /api/v1/group-docs/{id}/submission` |
| `GroupSubmissionView` | | `GET /api/v1/group-docs/{id}/submission` |
| Tải DOCX | | `GET /api/v1/group-docs/{id}/export.docx?submission=` |
