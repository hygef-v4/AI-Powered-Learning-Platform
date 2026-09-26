# U13 AI & Code Execution - Business Logic Model

## F1 - Tạo đề xuất câu hỏi
1. Nhận yêu cầu (U08/U06/U10), kiểm phạm vi và tham số (BR-U13-10…13).
2. `AiGuard`: kill-switch, trần ngày, rate limit, `CreditPort.reserve` ước tính (BR-U13-03).
3. Tạo `AiProposal` `QUEUED` + job `U13_AI_TASK`.
4. Worker: `RagRetrievalPort.retrieve(scope, query, k, requesterId, requestRef)` tính credit embedding của người yêu cầu qua U05 → dựng prompt (khối dữ liệu phân cách) → `AiGateway.generate(task, prompt, jsonSchema)` → kiểm JSON + quy tắc U06 → `READY`; `settle` credit tạo nội dung; ghi `AiCall` (BR-U13-04…09, 14). Nếu truy xuất bị từ chối trước khi gọi Gemini thì `release` phần credit tạo nội dung đã giữ.
5. Giảng viên chọn câu → đích lưu nháp → `ACCEPTED` (BR-U13-15).

## F2 - Đề xuất chấm
1. U15 gọi `AiGradingPort.request(submissionRef)` (bài cá nhân, hoặc phần đóng góp của một thành viên trong bài nhóm); kiểm quyền giảng viên, `AiGuard`.
2. Worker: lấy nội dung (U11, hoặc các mục của thành viên từ U14), văn bản phẳng + XML rút gọn (U09) hoặc code + kết quả test; rubric (U06) → prompt → kiểm JSON → `RubricPort.score` → `READY` (BR-U13-20…23).

## F3 - Kiểm lời giải mẫu
1. Giảng viên bấm "Kiểm lời giải mẫu" → `CodeRun` `VERIFY` chạy mọi test với cùng giới hạn như bài nộp.
2. Lưu `CodeRun` loại `VERIFY` kèm `contentHash`; đạt hết test thì bài/câu được coi là đã kiểm (`SolutionVerification` suy ra); `CodeLabCheckPort` cho U08 dùng khi duyệt (BR-U13-33).

## F4 - Chạy thử và chấm code
1. `TRY`: rate limit, chỉ test công khai, trả kết quả (BR-U13-34, 36).
2. `GRADE`: event nộp (`u11.submission.submitted` với bài `CODE_LAB`) → job chạy mọi test → điểm xác định → event `CODE_GRADED` cho U15 (BR-U13-35).
3. Judge0 lỗi → retry job; hết lượt → `SANDBOX_ERROR`, U15 hiện "chưa chấm được" (BR-U13-31, 37).

## F5 - Quản trị AI
1. ADMIN sửa cấu hình, kill-switch (BR-U13-40).
2. Báo cáo từ `AiCall` (BR-U13-41).
