# U14 Group Document & Submission - Domain Entities

## 1. Phạm vi sở hữu

U14 sở hữu tài liệu nhóm của từng nhóm, các mục việc, khóa mục, lịch sử phiên bản mục theo tác giả, bình luận review và bản nộp bài nhóm. U14 **không** sở hữu: bộ nhóm và trưởng nhóm (U12), bài/khung (U08, U09), điểm (U15). Không còn "phần" do giảng viên phân công hay composite do giảng viên chốt.

## 2. `GroupDocument`

`id`, `publicationId`, `groupId` (duy nhất theo cặp), `sharedBlocks` (các block ngoài mục: phần khóa của giảng viên như trang bìa, lời dẫn), `updatedAt`, `version`.

## 3. `Section` (mục việc)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `groupDocumentId` | UUID | |
| `orderNo` | số | |
| `title` | chuỗi ≤ 200 | |
| `origin` | enum | `TEACHER` (từ heading `workSection` của khung), `GROUP` (nhóm thêm) |
| `status` | enum | `OPEN`, `CLAIMED`, `IN_REVIEW` |
| `claimedBy`, `claimedAt` | | Khi `CLAIMED` |
| `publishedBlocks` | JSON | Nội dung đang hiển thị trong tài liệu chung (mô hình U09) |
| `draftBlocks` | JSON | Bản đang làm của người nhận (chỉ người nhận thấy) |
| `lastAuthorId`, `version` | | |

## 4. `SectionRevision`

`id`, `sectionId`, `authorId`, `blocks` (bất biến), `createdAt` — tạo mỗi lần bấm "Xong". Dùng để biết ai viết gì.

## 5. `SectionComment`

`id`, `sectionId`, `authorId`, `text` (≤ 2 000), `createdAt`, `resolvedAt`.

## 6. `GroupSubmission`

`id`, `groupDocumentId`, `submittedBy` (trưởng nhóm, hoặc `SYSTEM`), `submitMode` (`MANUAL`, `AUTO_DEADLINE`, `AUTO_RETIRED`), `document` (toàn bộ tài liệu bất biến), `sectionAuthors` (mục → danh sách tác giả theo `SectionRevision`), `warnings`, `late`, `submittedAt`, `receiptHash`. Nhiều lần nộp; lần cuối được chấm.

## 7. Trạng thái mục

```
OPEN --nhận--> CLAIMED --Xong--> IN_REVIEW --nhận lại (ai trong nhóm)--> CLAIMED
  ^              |
  +---nhả khóa---+   (người nhận tự nhả; trưởng nhóm/giảng viên nhả; người nhận rời nhóm)
```

**Text alternative**: Mục trống (`OPEN`) được một thành viên nhận thì thành `CLAIMED` và bị khóa cho người đó; bấm "Xong" thì nội dung vào tài liệu chung, mục thành `IN_REVIEW` và hết khóa; bất kỳ thành viên nào cũng có thể nhận lại mục đang review để sửa. Mục đang `CLAIMED` được nhả về `OPEN` (nếu chưa từng xong) hoặc `IN_REVIEW` (nếu đã có nội dung).

## 8. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `GroupSubmissionQueryPort` | U14 cung cấp cho U13, U15, U16 | Bản nộp, tác giả từng mục, các mục của một thành viên |
| Event `u14.group.submitted` | U14 phát | Cho U15, U16 |
| `GroupMembershipPort` | U14 dùng U12 | Nhóm, thành viên, trưởng nhóm |
| `AssignmentQueryPort`, `isSubmissionOpen` | U14 dùng U08 | Khung, hạn |
| `DocumentModelPort`, `DocxExportPort`, `DocumentEditor` | U14 dùng U09 | Kiểm/làm sạch block, xuất DOCX, trình soạn |
| `ArtifactPort` | U14 dùng U03 | Ảnh trong mục |
| `JobPort`, `AuditPort`, `EventPublisherPort` | U14 dùng U02 | Tự nộp, audit, event |
