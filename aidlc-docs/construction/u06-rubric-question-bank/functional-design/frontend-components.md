# U06 Rubric & Question Bank - Frontend Components

```
app/teaching/bank/                 BankPage (chọn phạm vi: môn hoặc lớp)
  BankFilters                      loại, độ khó, tag, chương/bài, trạng thái, từ khóa
  BankItemTable                    tiêu đề, loại, phiên bản, trạng thái, hành động
  QuestionEditor
    McqEditor                      lựa chọn 2-6, chọn đáp án đúng
    EssayEditor
    DocumentQuestionEditor         DocumentEditor (U09, chế độ soạn khung) + sơ đồ bắt buộc
    CodeEditor                     ngôn ngữ, code mẫu, bảng test case
    ClassificationFields           độ khó, tag, chương/bài (U05)
  RubricEditor                     tiêu chí → mục checklist + điểm, tổng tự tính
  VersionHistoryDrawer
  PreviewDialog                    xem như người học, bật/tắt đáp án
  CloneDialog                      chọn phạm vi đích
  ImportDialog                     chọn loại, tải mẫu, chọn file, bảng kết quả từng dòng
```

| Component | Hành vi | API |
|---|---|---|
| `BankItemTable` | Phân trang 50, hiện bản `ACTIVE` mới nhất và cờ "có bản nháp" | `GET /api/v1/bank/items` |
| `QuestionEditor`, `RubricEditor` | Lưu nháp; nút Kích hoạt hiện lỗi theo trường | `POST`, `PATCH /api/v1/bank/items`, `POST .../{id}/activate` |
| `RubricEditor` | Thêm/xóa/sắp xếp tiêu chí và mục; tổng điểm cập nhật ngay | như trên |
| `VersionHistoryDrawer` | Danh sách phiên bản, xem từng bản | `GET /api/v1/bank/items/{stableKey}/versions` |
| `PreviewDialog` | Render bằng component hiển thị câu hỏi dùng chung với U11 | `GET /api/v1/bank/items/{id}/preview` |
| `ImportDialog` | 4 file mẫu tải về; kết quả từng dòng | `GET /api/v1/bank/import-templates/{type}`, `POST /api/v1/bank/imports` |
