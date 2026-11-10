# Đối chiếu AI-DLC với code staging 2026-11-10

Phạm vi: `staging@062a43f5` (trùng `origin/staging`), các commit code sau đợt sync 2026-11-07 và 2026-11-09. Chỉ đọc source và sửa Markdown; không chạy application test, không sửa code/schema/config. Thay đổi chưa commit trong working tree (Ctrl+Z khôi phục cả rubric và hướng dẫn ở `AssignmentFormPage`) nằm ngoài phạm vi.

## Đã đồng bộ từ trước (không sửa)

Excel import, Bật/Tắt học liệu, soạn quiz tại chỗ, nguồn AI (`fb8846ad`); thẻ góp ý Practice (`79309e34`); U04/U13/U14 (`fc31721b`); U01/U10/U11 (`4531b60e`); các thay đổi U12 group document có tài liệu đi kèm commit.

## Khoảng cách tìm thấy và tài liệu đã sửa

| Unit | Code (commit) | Hiện trạng code | Tài liệu đã sửa |
|---|---|---|---|
| U07 | `9cf5d502`, `594c79e5` | Khung chat "Sửa đề bằng AI" (`AiReviseChat`), `mode = REVISE` trên `POST /assignments/{id}/ai-drafts`; AI trả thao tác theo mã khối, server ghép vào bản đã gửi; `409 AI_DRAFT_STALE`, `409 AI_DRAFT_INVALID`; chat chờ tối đa 3 phút. | [logic F6b](../u07-assignment-authoring/functional-design/business-logic-model.md), [BR-U07-54, 55](../u07-assignment-authoring/functional-design/business-rules.md), [entities](../u07-assignment-authoring/functional-design/domain-entities.md), [frontend](../u07-assignment-authoring/functional-design/frontend-components.md), [as-built](../u07-assignment-authoring/code/as-built-baseline.md) |
| U08 | `af69df96` | Nộp tay không còn chặn theo `validateForSubmit`; popup chỉ xác nhận; xóa `ReceiptPage` và route biên nhận; nộp xong về chi tiết bài. | [BR-U08-20, 22](../u08-assignment-work/functional-design/business-rules.md), [logic F6](../u08-assignment-work/functional-design/business-logic-model.md), [frontend](../u08-assignment-work/functional-design/frontend-components.md), [as-built](../u08-assignment-work/code/as-built-baseline.md) |
| U09 | `d85b5429`, `fb20c6bc`, `baf349b0` | Phần bỏ trống nhận 0 điểm không gửi AI; bài trống hoàn toàn có đề xuất `READY` ngay; chuẩn hóa payload AI; `groupEvaluationId`; "Bản nộp chung theo nhóm" trong Sổ điểm. | [BR-U09-36](../u09-assignment-grading/functional-design/business-rules.md), [logic F5, F14](../u09-assignment-grading/functional-design/business-logic-model.md), [frontend](../u09-assignment-grading/functional-design/frontend-components.md), [as-built](../u09-assignment-grading/code/as-built-baseline.md) |
| U10 | `af5bb406` | `bank-tags` trả `byDifficulty`; `DrawRulesPanel` cảnh báo thiếu câu, không chặn lưu. | [logic](../u10-quiz-authoring/functional-design/business-logic-model.md), [as-built](../u10-quiz-authoring/code/as-built-baseline.md); frontend-components đã đúng từ commit gốc |
| U06 | `89029b0b` | Thêm khóa Gemini chọn sẵn `gemini-3.1-flash-lite` khi catalog có. | [frontend](../u06-ai-connections/functional-design/frontend-components.md) |
| U05, U01 | `cc99aaf9` | Môn/lớp hiện thành cây `NavTree` dưới mục menu cha qua `NavContext.parentHref`. | [U05 frontend](../u05-class-enrollment/functional-design/frontend-components.md), [U01 frontend](../u01-account-and-access/functional-design/frontend-components.md) |
| U04 | `0df689df` | `SubjectExceptionHandler` log exception gốc, response không đổi. | [as-built](../u04-subject-administration/code/as-built-baseline.md) |
| Toàn dự án | `c9a2d378` và các commit trên | Metadata Open Graph, file frontend mới/đã xóa; 3 migration và 12 test class chưa có trong bảng kê; 2 liên kết tới file groupdocs đã xóa. | [current-code-baseline](../../inception/reverse-engineering/current-code-baseline.md), [api-inventory](../../inception/reverse-engineering/api-inventory.md), [evidence matrix](build-and-test-summary.md), [state](../../aidlc-state.md) |

Không cần tài liệu: `6f6aa5d0` (cấu hình dev server cục bộ), `e6a5780c`, `7efb2b21`, `19e8ebf6`, `0f2fbab7` (chỉ comment, format, import).

## Kiểm tra đã làm

- API inventory: 239 mapping annotation khác nhau trong 48 controller khớp hoàn toàn với source.
- current-code-baseline: mọi controller, test class (`*Test.java`) và migration trong source đều có trong bảng kê; không còn liên kết hỏng.
- Liên kết cục bộ của các file Markdown đã sửa trong đợt này đều trỏ tới file tồn tại.

## Giới hạn

- Các `code-summary.md` có ngày của U07/U08 giữ mô tả cũ làm lịch sử (ví dụ trang biên nhận, kiểm sơ bộ trước khi nộp); hiện trạng nằm ở as-built và functional-design.
- Chỉ đối chiếu theo commit code từ 2026-11-03 tới nay và hai bảng kê; không đọc lại toàn bộ rule của U02, U03, U11–U15 so với source.
- `doc-code-traceability.md` (snapshot 2026-10-30) không sửa: các rule mới BR-U07-54, 55 và BR-U09-36 thuộc UC 35 và UC 43 đã có dòng truy vết.
- Requirements, use case và stories không đổi vì các thay đổi nằm trong UC/FR hiện có. Story US-ASM-003 vẫn nhắc "receipt" ở nghĩa mã biên nhận của lượt nộp, backend vẫn lưu.
- Backend còn `GET /api/v1/attempts/{attemptId}/receipt` và `submitIssues` không ai dùng; đây là ghi nhận, không phải đề xuất đã duyệt.
