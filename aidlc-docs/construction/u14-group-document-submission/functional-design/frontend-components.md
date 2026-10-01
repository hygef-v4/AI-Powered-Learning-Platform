# U14 Group Document & Submission - Frontend Components

```
app/learn/group-docs/[id]/                  GroupDocumentPage (tài liệu chung)
  GroupDocHeader            tên nhóm, trưởng nhóm, hạn, trạng thái tài liệu (Đang làm / Review), trạng thái nộp, nút Nộp (trưởng nhóm, chỉ khi Review), Tải DOCX
  SectionOutline            cây mục chính/mục chi tiết: trạng thái (Trống / Đang làm bởi X / Xong), người giao, nút Nhận
  SharedBlocksView          phần chung của khung (chỉ đọc)
  SectionView               publishedBlocks (chỉ đọc) + bình luận
  ManageSectionsDialog      (trưởng nhóm) thêm/sửa/đổi thứ tự/xóa mục chi tiết, giao mục cho thành viên
  ReleaseLockButton         (trưởng nhóm; giảng viên ở trang giảng viên)
  ReviewBanner              báo tài liệu đã vào Review, mời cả nhóm xem lại và bình luận
  SubmitGroupDialog         cảnh báo bình luận chưa giải quyết
  useGroupDocStream         kết nối SSE, áp dụng sự kiện, tải lại khi kết nối lại
app/learn/group-docs/[id]/sections/[sectionId]   SectionWorkPage
  DocumentEditor (U09, mode STUDENT, chỉ block của mục), "Đã lưu lúc …", nút Xong, nút Nhả
shared/group-docs/GroupDocsOverviewPanel       (giảng viên; hiện trong danh sách bài nộp của publication khi bài là GROUP, thay bảng theo từng sinh viên)
  bảng nhóm: trạng thái tài liệu, số mục xong/đang làm/trống, bản nộp, xem tài liệu (mở chi tiết bài nộp của nhóm), nhả khóa
```

| Component | Hành vi | API |
|---|---|---|
| `GroupDocumentPage` | | `GET /api/v1/group-docs/{id}` |
| `useGroupDocStream` | SSE; mất kết nối → thử lại, tải lại toàn bộ | `GET /api/v1/group-docs/{id}/events` (SSE) |
| `SectionOutline` Nhận | | `POST /api/v1/group-docs/{id}/sections/{sid}/claim` |
| `ManageSectionsDialog` | Chỉ trưởng nhóm | `POST /api/v1/group-docs/{id}/sections`, `PATCH`, `DELETE .../sections/{sid}`, `POST .../sections/{sid}/assign` |
| `SectionWorkPage` | Tự lưu 10 s | `PUT .../sections/{sid}/draft`, `POST .../done`, `POST .../release` |
| Bình luận | | `POST .../sections/{sid}/comments`, `POST .../comments/{cid}/resolve` |
| `SubmitGroupDialog` | Bật khi tài liệu `REVIEW` | `POST /api/v1/group-docs/{id}/submissions` |
| Tải DOCX | | `GET /api/v1/group-docs/{id}/export.docx?submission=` |
