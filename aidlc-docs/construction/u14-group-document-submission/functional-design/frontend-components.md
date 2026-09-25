# U14 Group Document & Submission - Frontend Components

```
app/learn/group-docs/[id]/                  GroupDocumentPage (tài liệu chung)
  GroupDocHeader            tên nhóm, trưởng nhóm, hạn, trạng thái nộp, nút Nộp (trưởng nhóm), Tải DOCX
  SectionOutline            danh sách mục: trạng thái (Trống / Đang làm bởi X / Chờ review), nút Nhận
  SharedBlocksView          phần chung của khung (chỉ đọc)
  SectionView               publishedBlocks (chỉ đọc) + bình luận
  AddSectionDialog          thêm mục nhóm
  ReleaseLockButton         (trưởng nhóm; giảng viên ở trang giảng viên)
  SubmitGroupDialog         cảnh báo mục chưa xong
  useGroupDocStream         kết nối SSE, áp dụng sự kiện, tải lại khi kết nối lại
app/learn/group-docs/[id]/sections/[sectionId]   SectionWorkPage
  DocumentEditor (U09, mode LEARNER, chỉ block của mục), "Đã lưu lúc …", nút Xong, nút Nhả
app/teaching/assignments/[id]/group-docs/        GroupDocsOverviewPage (giảng viên)
  bảng nhóm: số mục xong/đang làm/trống, bản nộp, xem tài liệu, nhả khóa
```

| Component | Hành vi | API |
|---|---|---|
| `GroupDocumentPage` | | `GET /api/v1/group-docs/{id}` |
| `useGroupDocStream` | SSE; mất kết nối → thử lại, tải lại toàn bộ | `GET /api/v1/group-docs/{id}/events` (SSE) |
| `SectionOutline` Nhận | | `POST /api/v1/group-docs/{id}/sections/{sid}/claim` |
| `SectionWorkPage` | Tự lưu 10 s | `PUT .../sections/{sid}/draft`, `POST .../done`, `POST .../release` |
| Bình luận | | `POST .../sections/{sid}/comments`, `POST .../comments/{cid}/resolve` |
| `SubmitGroupDialog` | | `POST /api/v1/group-docs/{id}/submissions` |
| Tải DOCX | | `GET /api/v1/group-docs/{id}/export.docx?submission=` |
