# U13 AI & Code Execution - Frontend Components

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding học liệu (UC 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

U13 không có màn riêng trong screen flow: bỏ màn AI Usage và popup AI Setting (cấu hình AI là mục nhóm AI trên Setting List/Setting Detail của U03; số liệu AI hiện trên Admin Dashboard của U16). U13 cung cấp component dùng chung gắn vào màn của unit khác.

## 1. Màn có component của U13

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Codelab Workspace (U11) | `CodeEditor`, `CodeRunResult` | 25 | Student | Assignment Detail |
| Assignment Form (U08, U09) | `VerifySolutionButton`, `CodeRunResult` trong `CodeLabCheckPanel` của U09 | 43 | Teacher (bài của lớp), Subject Manager (bài của môn) | Teacher Class Detail, Assignment List |
| Assignment Form (U08) | `AiDraftDialog` (câu Text Essay, Code Lab) | 42, 43 (luồng phụ) | Teacher, Subject Manager | Teacher Class Detail, Assignment List |
| Quiz Detail (U08) | `AiDraftDialog` (câu quiz) | 35 (luồng phụ) | Teacher, Subject Manager | Teacher Class Detail, Quiz List |
| Question List (U06) | `AiDraftDialog` bên trong `AiQuestionDraftDialog` của U06 | 57 (luồng phụ) | Subject Manager | Manager Dashboard |

Không dùng component U13 nhưng gọi API/port của U13: Submission History (U11, nút "Chấm với AI" của Student, UC 29), Grading Workspace (U15, panel AI đề xuất, UC 38) và Student Submissions (U15, chấm hàng loạt, UC 38), Setting List/Setting Detail (U03, mục nhóm AI, UC 70–71), Admin Dashboard (U16, số liệu AI, UC 58), My Credit Package (U07, lần dùng credit, UC 08), form tải học liệu (U05, mức giữ credit, UC 34, 55).

## 2. Cây component

```
shared/ai/
  AiDraftDialog            tham số (loại câu, số câu, độ khó, module/học liệu, ghi chú), credit ước tính,
                           poll đề xuất, danh sách câu đề xuất + trích dẫn, chọn và sửa câu giữ lại
    AiErrorNotice          phân biệt "Không đủ credit AI", "Hệ thống đang bận", "AI đang tắt"
shared/codelab/
  CodeEditor               nhiều file, tô màu cú pháp 7 ngôn ngữ (Monaco)
  CodeRunResult            bảng test: trạng thái, thời gian, bộ nhớ; test ẩn chỉ đạt/không
  VerifySolutionButton     kiểm lời giải mẫu một câu CODE của bài, poll kết quả
```

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `AiDraftDialog` | Ẩn khi không có quyền; hiện credit ước tính trước khi gửi; poll 3 giây (`usePollStatus` của U03) tới `READY`/`FAILED`; câu giữ lại do màn chủ lưu (U08 qua `POST /api/v1/assignments/{id}/ai-drafts` rồi lưu câu riêng, U06 lưu câu nháp ngân hàng); bỏ thì gọi discard | Bài/quiz: `POST /api/v1/assignments/{id}/ai-drafts` (U08); ngân hàng: `POST /api/v1/ai/question-drafts`; chung: `GET /api/v1/ai-suggestions/{id}`, `POST /api/v1/ai-suggestions/{id}/accept`, `POST .../discard` |
| `CodeEditor` + `CodeRunResult` | Student chạy thử test công khai trên Codelab Workspace, 5 lần/phút; lỗi `429` báo chờ; không trừ credit (UC 25) | `POST /api/v1/code-runs` (`TRY`) |
| `VerifySolutionButton` | Trong `CodeLabCheckPanel` (U09) cho từng câu `CODE`; ẩn khi bài không còn `DRAFT`; hiện "Đạt", "Không đạt", "Cần kiểm lại" (nội dung đã sửa), "Chưa chạy được" (sandbox lỗi) (UC 43) | `POST /api/v1/code-runs` (`VERIFY`, `assignmentId`, `questionId`), `GET /api/v1/code-runs/{runId}` |

AI soạn khung dùng `AiSkeletonDraftDialog` của U09. Chấm AI không dùng component của U13: nút chấm Practice của Student là của U11 trên Submission History (UC 29), gọi API của U11; AI đề xuất chấm của giảng viên là panel của U15 trong Grading Workspace (UC 38), gọi API của U15 rồi poll `GET /api/v1/ai-suggestions/{id}`; chấm hàng loạt trên Student Submissions poll `GET /api/v1/ai-suggestions?ids=`. Không có route `app/admin/ai/`.
