# U13 AI & Code Execution - Frontend Components

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

```
shared/ai/
  AiDraftDialog            (dùng trong Assignment Editor U08, Question Bank U06, Template Editor U10) tham số, credit ước tính, poll, danh sách câu đề xuất + trích dẫn, chọn câu giữ lại
shared/codelab/
  CodeEditor               nhiều file, tô màu cú pháp (Monaco)
  CodeRunResult            bảng test: trạng thái, thời gian, bộ nhớ; test ẩn chỉ đạt/không
  VerifySolutionButton     (dùng trong trình soạn câu CODE ở Question Bank và `CodeLabConfigForm` của U09 ở Assignment Editor, Template Editor)
app/admin/ai/
  AiUsagePage              màn AI Usage mở từ Admin Sidebar: lượt, latency, lỗi, token, chi phí theo ngày/việc/model
    AiSettingsDialog       popup AI Setting trên AI Usage: model theo từng việc, trần chi phí ngày, giới hạn/phút, kill-switch
```

Popup Grade with AI (UC 25, 35) là `GradeWithAiDialog` của U11 (screen flow ghi U11), dùng cả phía giảng viên; giảng viên gửi yêu cầu qua U15 (`POST /api/v1/evaluations/{id}/ai-proposal`, `POST /api/v1/evaluations:ai-proposal-batch`; U15 gọi `AiGradingPort`), U13 chỉ cung cấp `GET /api/v1/ai-suggestions/{id}` để poll.

| Component | Hành vi | API |
|---|---|---|
| `AiDraftDialog` | Hiện credit ước tính trước khi gửi; poll job; phân biệt "Không đủ credit AI" với "Hệ thống đang bận". Assignment Editor gửi qua U08 (`POST /api/v1/assignments/{id}/ai-drafts`); khung Diagram Essay, bài nhóm dùng `AiSkeletonDraftDialog` của U09 | `POST /api/v1/ai/question-drafts` (Question Bank, Template Editor), `GET /api/v1/ai-suggestions/{id}`, `POST .../{id}/accept` |
| `CodeEditor` + `CodeRunResult` | Chạy thử (người học), 5 lần/phút | `POST /api/v1/code-runs` (`TRY`), `GET /api/v1/code-runs/{id}` |
| `VerifySolutionButton` | | `POST /api/v1/code-runs` (`VERIFY`) |
| `AiUsagePage` | | `GET /api/v1/admin/ai/usage?from=&to=` |
| `AiSettingsDialog` | Mở từ AiUsagePage | `GET`, `PUT /api/v1/admin/ai/settings` |
