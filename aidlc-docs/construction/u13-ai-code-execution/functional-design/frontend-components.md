# U13 AI & Code Execution - Frontend Components

```
shared/ai/
  AiDraftDialog            (dùng trong U08, U06, U10) tham số, credit ước tính, poll, danh sách câu đề xuất + trích dẫn, chọn câu giữ lại
  AiGradingPanel           (dùng trong U15) nút "Nhờ AI đề xuất", trạng thái, checklist đạt/không + nhận xét + bằng chứng, cờ nghi chèn lệnh
shared/codelab/
  CodeEditor               nhiều file, tô màu cú pháp (Monaco)
  CodeRunResult            bảng test: trạng thái, thời gian, bộ nhớ; test ẩn chỉ đạt/không
  VerifySolutionButton     (dùng trong trình soạn câu CODE của U06/U08)
app/admin/ai/
  AiSettingsPage           model theo từng việc, trần chi phí ngày, giới hạn/phút, kill-switch
  AiUsageDashboard         lượt, latency, lỗi, token, chi phí theo ngày/việc/model
```

| Component | Hành vi | API |
|---|---|---|
| `AiDraftDialog` | Hiện credit ước tính trước khi gửi; poll job; phân biệt "Không đủ credit AI" với "Hệ thống đang bận" | `POST /api/v1/ai/question-drafts`, `GET /api/v1/ai/proposals/{id}`, `POST .../{id}/accept` |
| `AiGradingPanel` | Hiện riêng lỗi thiếu credit và hệ thống bận | `POST /api/v1/ai/grading-proposals`, `GET /api/v1/ai/proposals/{id}` |
| `CodeEditor` + `CodeRunResult` | Chạy thử (người học), 5 lần/phút | `POST /api/v1/code-runs` (`TRY`), `GET /api/v1/code-runs/{id}` |
| `VerifySolutionButton` | | `POST /api/v1/code-runs` (`VERIFY`) |
| `AiSettingsPage` | | `GET`, `PUT /api/v1/admin/ai/settings` |
| `AiUsageDashboard` | | `GET /api/v1/admin/ai/usage?from=&to=` |
