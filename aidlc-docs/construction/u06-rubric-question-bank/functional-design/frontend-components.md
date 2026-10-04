# U06 Rubric & Question Bank - Frontend Components

```
app/teaching/bank/                 BankPage (chọn phạm vi: môn hoặc lớp)
  BankFilters                      dạng bài, loại câu, độ khó, tag, module/học liệu, trạng thái, từ khóa
  BankItemTable                    tiêu đề, loại, phiên bản, trạng thái, hành động
  QuestionEditor
    McqEditor                      lựa chọn 2-6, chọn đáp án đúng
    EssayEditor
    DocumentQuestionEditor         DocumentEditor (U09, chế độ soạn khung) + sơ đồ bắt buộc; khung dùng kiểu heading giống Word; phần tự tính theo heading nhỏ nhất
    CodeEditor                     ngôn ngữ, code mẫu, bảng test case
    ClassificationFields           độ khó, tag, module/học liệu (U05)
  RubricEditor                     tiêu chí → mục checklist + điểm, tổng tự tính; ở Question Bank chỉ mở rubric đã có để sửa (không có nút tạo); U09 dùng lại component này trong `QuestionRubricPanel`, `PartRubricPanel` (Assignment Editor, Template Editor) để tạo rubric khi soạn đề
  VersionHistoryDrawer
  QuestionView                     hiển thị câu như người học thấy (ẩn đáp án, test ẩn); dùng chung cho U08, U11, U15
  PreviewDialog                    xem như người học, bật/tắt đáp án
  CloneDialog                      chọn phạm vi đích (chỉ câu hỏi; rubric nhân bản theo đề)
  ImportDialog                     chọn loại, tải mẫu, chọn file, bảng kết quả từng dòng
```

| Component | Hành vi | API |
|---|---|---|
| `BankItemTable` | Phân trang 50, hiện bản `ACTIVE` mới nhất và cờ "có bản nháp" | `GET /api/v1/bank/items` |
| `QuestionEditor`, `RubricEditor` | Lưu nháp; nút Kích hoạt hiện lỗi theo trường. Question Bank không có nút "Tạo rubric" | `POST`, `PATCH /api/v1/bank/items`, `POST .../{id}/activate` |
| `RubricEditor` | Thêm/xóa/sắp xếp tiêu chí và mục; tổng điểm cập nhật ngay; lưu tạo phiên bản mới (chỉ bài còn `DRAFT` chuyển sang bản mới; bài đã duyệt (`REVIEWED`) hoặc đã phát hành giữ phiên bản rubric đã ghim, theo BR-U06-36) | như trên |
| `VersionHistoryDrawer` | Danh sách phiên bản, xem từng bản | `GET /api/v1/bank/items/{lineageId}/versions` |
| `PreviewDialog` | Render bằng component hiển thị câu hỏi dùng chung với U11 | `GET /api/v1/bank/items/{id}/preview` |
| `ImportDialog` | 4 file mẫu tải về; kết quả từng dòng | `GET /api/v1/bank/import-templates/{type}`, `POST /api/v1/bank/imports` |
