# U06 Rubric & Question Bank - Frontend Components

**Bản tài liệu 2026-10-09**: UC 46, 56, 57 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-QBK-001, US-QBK-002. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Màn hình theo screen flow

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Question List | `QuestionListPage` | 56, 57 | Subject Manager | Manager Dashboard |
| Question Detail | `QuestionDetailPage` | 56, 57 | Subject Manager | Question List |
| Rubric Detail (popup) | `RubricEditor` do U06 cung cấp, U09 gắn vào Assignment Form | 46 | Teacher, Subject Manager | Assignment Form |

## 2. Cây component

```
app/manager/questions/                 QuestionListPage    màn Question List
  SubjectPicker                        chọn môn mình quản lý
  QuestionFilters                      dạng bài, loại câu, độ khó, tag, module/học liệu, trạng thái, từ khóa
  QuestionTable                        tiêu đề, loại, phiên bản, trạng thái, cờ có bản nháp
  ImportDialog                         chọn loại câu, tải file mẫu, chọn file, bảng kết quả từng dòng
  AiQuestionDraftDialog                nhờ AI soạn câu theo học liệu, chọn và sửa câu giữ lại
app/manager/questions/new/             QuestionDetailPage  tạo câu mới
app/manager/questions/[id]/            QuestionDetailPage  màn Question Detail
  QuestionActions                      Sửa, Kích hoạt, Ngưng dùng, Xóa theo trạng thái
  VersionHistoryPanel                  danh sách phiên bản, xem từng bản
  PreviewDialog                        xem như người học, bật/tắt đáp án
shared/question/
  QuestionEditor                       theo loại; U08 dùng lại trong Quiz Detail và Assignment Form cho câu riêng của bài
    McqEditor                          2-6 lựa chọn, đáp án đúng, giải thích
    EssayEditor                        đề, gợi ý đáp án
    DocumentQuestionEditor             DocumentEditor (U09, chế độ khung) + sơ đồ bắt buộc
    CodeEditor                         ngôn ngữ, code mẫu, lời giải mẫu, bảng test case
  ClassificationFields                 độ khó, tag, module/học liệu (U05)
  QuestionView                         hiển thị câu như người học (ẩn đáp án); dùng chung cho U09, U11, U15
  QuestionPicker                       tìm và chọn câu ACTIVE khớp dạng bài; U08 gắn vào Quiz Detail và Assignment Form; U09 dùng để lấy khung DOCUMENT
shared/rubric/
  RubricEditor                         tiêu chí → mục checklist + điểm, tổng tự tính, cảnh báo khi trống; U09 gắn vào popup Rubric Detail
  RubricView                           rubric chỉ đọc (đã khóa); dùng chung cho U11, U15
```

Route theo `RoleGuard` của U01: `/manager/*` cho Subject Manager. Teacher không có màn ngân hàng; chỉ dùng `QuestionPicker` trong Quiz Detail và Assignment Form. Backend vẫn kiểm Chủ nhiệm môn hoặc giảng viên của lớp thuộc môn.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `QuestionListPage`, `QuestionFilters`, `QuestionTable` | Phân trang 50, hiện bản `ACTIVE` mới nhất, cờ "có bản nháp" và câu chỉ có bản nháp; nút Tạo câu hỏi, Nhập từ file, Nhờ AI soạn câu hỏi (UC 56) | `GET /api/v1/subjects/{subjectId}/questions` |
| `QuestionDetailPage`, `QuestionEditor`, `ClassificationFields` | Xem câu kèm đáp án; lưu nháp; nút Kích hoạt hiện lỗi theo trường; sửa bản `ACTIVE` báo sẽ tạo phiên bản mới (UC 56, 57) | `GET`, `PATCH /api/v1/questions/{id}`, `POST /api/v1/subjects/{subjectId}/questions` |
| `QuestionActions` | Kích hoạt, Ngưng dùng (bản `ACTIVE`), Xóa (bản nháp chưa từng kích hoạt), có hộp xác nhận (UC 57) | `POST .../activate`, `POST .../retire`, `DELETE /api/v1/questions/{id}` |
| `VersionHistoryPanel` | Danh sách phiên bản, xem từng bản | `GET /api/v1/questions/{id}/versions` |
| `PreviewDialog`, `QuestionView` | Render câu như người học thấy | `GET /api/v1/questions/{id}/preview` |
| `ImportDialog` | Tải file mẫu; kết quả từng dòng | `GET /api/v1/question-import-template?type=&format=`, `POST /api/v1/subjects/{subjectId}/question-imports` |
| `AiQuestionDraftDialog` | Chọn loại câu, module/học liệu, số câu, độ khó; hiện đề xuất kèm trích dẫn; lưu câu giữ lại thành nháp; báo "Không đủ credit AI" hoặc "Hệ thống đang bận" | API U13 `POST /api/v1/ai/question-drafts`, `/ai-suggestions/{id}/accept`; `POST /api/v1/subjects/{subjectId}/questions` |
| `QuestionPicker` | Tìm câu `ACTIVE` của môn khớp dạng bài để thêm vào bài hoặc quiz, xem trước | `GET /api/v1/subjects/{subjectId}/questions?status=ACTIVE`, `GET .../preview` |
| `RubricEditor` | Rubric đã được tự tạo cho câu hoặc phần; thêm/xóa/sắp xếp tiêu chí và mục; tổng điểm cập nhật ngay; nút Lưu, không có nút xóa rubric; rubric trống hiện "Cần điền rubric trước khi phát hành"; rubric đã khóa hiện chỉ đọc (UC 46) | API rubric của U09 (`/assignments/{id}/questions/{questionId}/rubric`, `/assignments/{id}/parts/{partId}/rubric`) |

Không còn Class Question Bank, `CloneDialog`, nút sửa rubric trong ngân hàng và nút tạo/xóa rubric (bỏ 2026-10-09).
