# U11 Attempt & Submission - Domain Entities

## 1. Phạm vi sở hữu

U11 sở hữu lượt làm cá nhân (attempt), bản nháp, bài nộp, biên nhận và lịch sử. U11 **không** sở hữu: bài/publication (U08), cấu hình loại bài và mô hình tài liệu (U09), chính sách thi thử (U10), phần bài nhóm (U14), chạy code (U13), điểm (U15).

## 2. `Attempt` (bảng `submissions`, một dòng một lượt)

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `publicationId` | UUID | |
| `learnerId` | UUID | |
| `attemptNo` | số | Duy nhất theo `(publicationId, learnerId)` |
| `assignmentVersionId` | UUID | Version bài lúc bắt đầu (bất biến, U08 `LOCKED`) |
| `snapshot` | JSON | Cấu hình loại bài, chính sách (hạn, nộp trễ, giờ làm, thi thử), seed trộn câu/đáp án, thứ tự câu |
| `status` | enum | `IN_PROGRESS`, `SUBMITTED` |
| `submitMode` | enum | `MANUAL`, `AUTO_TIME_LIMIT`, `AUTO_DEADLINE`, `AUTO_RETIRED` |
| `late` | bool | Nộp sau `closesAt` trong thời gian cho phép trễ |
| `startedAt`, `deadlineAt`, `lastSavedAt`, `submittedAt` | thời gian | `deadlineAt` = sớm nhất giữa (`startedAt` + giới hạn giờ) và hạn cuối nhận bài |
| `contentVersion` | số | Tăng mỗi lần lưu (chống ghi đè giữa hai tab) |
| `receiptHash` | chuỗi | SHA-256 nội dung lúc nộp |

## 3. `AttemptContent` (bảng `submission_contents`, 1-1 với `Attempt`)

| Loại bài | Nội dung |
|---|---|
| `QUIZ` | `answers`: câu → danh sách `optionId` đã chọn |
| `ESSAY` | Tài liệu (mô hình U09, chỉ block chữ) |
| `DOCUMENT` | Tài liệu (mô hình U09: khung + block người học; sơ đồ gồm XML + SVG; ảnh qua U03 `DOCUMENT_IMAGE`) |
| `CODE_LAB` | `files`: tên → nội dung; `language` |

Sau khi `SUBMITTED`, nội dung bất biến.

## 4. Trạng thái

```
(Bắt đầu) --> IN_PROGRESS --nộp / tự nộp--> SUBMITTED
```

**Text alternative**: Bấm "Bắt đầu làm" tạo lượt ở `IN_PROGRESS`; người học nộp hoặc hệ thống tự nộp (hết giờ, hết hạn, ngừng giao) chuyển sang `SUBMITTED`, sau đó không đổi được.

## 5. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| `SubmissionQueryPort` | U11 cung cấp cho U13, U14, U15, U16 | Bài nộp, nội dung, lượt được chấm |
| Event `SUBMISSION_SUBMITTED` | U11 phát | `{attemptId, publicationId, learnerId, late, submitMode}` cho U15, U16 |
| `AssignmentQueryPort`, `isSubmissionOpen` | U11 dùng U08 | Bài, publication, hạn |
| `TypeConfigPort`, `DocumentModelPort`, `DocxExportPort` | U11 dùng U09 | Cấu hình, kiểm tài liệu, xuất DOCX |
| `SimulationPolicyPort` | U11 dùng U10 | Lượt tối đa, khóa chính sách |
| `BankQueryPort` | U11 dùng U06 | Góc nhìn người học của câu hỏi |
| `CodeRunPort` | U11 dùng U13 (`C`) | Chạy thử code khi đang làm |
| `ClassAccessPort` | U11 dùng U04 | Ghi danh |
