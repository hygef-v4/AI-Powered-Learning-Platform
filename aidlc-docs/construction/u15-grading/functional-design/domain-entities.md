# U15 Grading - Domain Entities

## 1. Phạm vi sở hữu

U15 sở hữu điểm, phản hồi, trạng thái chốt/công bố, lịch sử thay đổi và sổ điểm. U15 **không** sở hữu: bài nộp (U11, U14), rubric (U06), đề xuất AI và chạy code (U13), chính sách thi thử (U10).

## 2. `Grade`

| Thuộc tính | Kiểu | Ràng buộc |
|---|---|---|
| `id` | UUID | |
| `targetKind` | enum | `ATTEMPT` (lượt U11), `GROUP_DOCUMENT` (bản nộp nhóm U14), `MEMBER_CONTRIBUTION` (phần đóng góp của một thành viên), `MEMBER_FINAL` (điểm cuối thành viên bài nhóm) |
| `targetId`, `learnerId`, `publicationId` | UUID | Theo loại |
| `method` | enum | `DETERMINISTIC` (trắc nghiệm, code), `MANUAL`, `AI_ASSISTED` |
| `maxScore` | numeric(6,2) | Tổng điểm của bài (thành phần/rubric) |
| `autoScore` | numeric(6,2) | Điểm tự chấm hoặc đề xuất AI (tham khảo) |
| `items` | JSON | Theo câu: điểm câu; theo rubric: mục checklist đạt/không |
| `finalScore` | numeric(6,2) | 0 ≤ `finalScore` ≤ `maxScore` |
| `feedback` | markdown ≤ 10 000 | |
| `aiProposalId` | UUID | Khi dùng AI |
| `status` | enum | `PENDING`, `DRAFT`, `FINALIZED`, `PUBLISHED` |
| `finalizedBy`, `finalizedAt`, `publishedAt`, `version` | | |

## 3. `GradeHistory`

`id`, `gradeId`, `changedBy` (hoặc `SYSTEM`), `previous`, `next` (điểm, trạng thái, phương thức), `reason`, `changedAt`. Bất biến.

## 4. `PublicationGradeRelease`

`publicationId`, `released` (bool), `releasedBy`, `releasedAt` — bấm "Công bố" cho một lượt phát hành.

## 5. Trạng thái

```
PENDING --tự chấm/chấm tay/nhận đề xuất--> DRAFT --chốt--> FINALIZED --công bố--> PUBLISHED
                                              ^                |                      |
                                              +----sửa (lý do)-+----------sửa (lý do)-+
```

**Text alternative**: Bài nộp mới tạo điểm `PENDING`; khi có điểm tự chấm, điểm chấm tay hoặc giảng viên nhận đề xuất AI thì thành `DRAFT`; giảng viên chốt thành `FINALIZED`; công bố thành `PUBLISHED`. Sửa điểm đã chốt hoặc đã công bố cần lý do và quay về `FINALIZED`/`PUBLISHED` với lịch sử lưu lại. Trắc nghiệm có "hiện điểm ngay sau nộp" đi thẳng tới `PUBLISHED`.

## 6. Contract

| Contract | Chiều | Mô tả |
|---|---|---|
| Event `u15.grade.published` | U15 phát | Cho U16 báo người học |
| `GradeQueryPort` | U15 cung cấp cho U11, U16 | Điểm đã công bố của một lượt/người học |
| `SubmissionQueryPort` | U15 dùng U11 | Lượt, nội dung, lượt được chấm |
| `GroupSubmissionQueryPort` | U15 dùng U14 | Bản nộp nhóm, mục theo tác giả |
| `BankQueryPort`, `RubricPort` | U15 dùng U06 | Đáp án (chấm trắc nghiệm), rubric, `score` |
| `AiGradingPort`, event `u13.code.graded` | U15 dùng U13 | Đề xuất chấm, điểm code |
| `SimulationPolicyPort` | U15 dùng U10 | Kết quả thi thử |
| `AssignmentQueryPort`, `TypeConfigPort` | U15 dùng U08, U09 | Bài, cấu hình hiện điểm |
