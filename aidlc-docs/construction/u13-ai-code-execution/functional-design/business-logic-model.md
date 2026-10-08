# U13 AI & Code Execution - Business Logic Model

**Bản tài liệu 2026-10-08**: không primary UC; primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## F1 - Tạo đề xuất câu hỏi
1. Nhận yêu cầu (U08/U06/U10 với câu hỏi; U09 với khung `SKELETON_DRAFT`), kiểm phạm vi và tham số (BR-U13-10…13).
2. `AiGuard`: kill-switch, trần ngày, rate limit, `CreditPort.reserve` ước tính (BR-U13-03).
3. Tạo dòng `ai_suggestions` `QUEUED` (`credit_status = RESERVED`) + việc `AI_TASK`.
4. Worker: `RagRetrievalPort.retrieve(scope, query, k, requesterId)` tính credit embedding của người yêu cầu (U05 gọi `AiUsagePort`, một dòng `ai_suggestions` riêng) → dựng prompt (khối dữ liệu phân cách) → `AiGateway.generate(task, prompt, jsonSchema)` → kiểm JSON + quy tắc U06 → `READY`; `settle` credit tạo nội dung; ghi token, chi phí, độ trễ vào dòng `ai_suggestions` (BR-U13-04…09, 14). Nếu truy xuất bị từ chối trước khi gọi Gemini thì `release` phần credit tạo nội dung đã giữ.
5. Người soạn chọn câu hoặc xác nhận khung → đích lưu nháp (U09 thay khung) → `ACCEPTED` (BR-U13-15).

## F2 - Đề xuất chấm
1. U15 gọi `AiGradingPort.request(submissionRef)` (bài cá nhân, hoặc tài liệu chung của bài nhóm như bài `DOCUMENT`); kiểm quyền giảng viên, `AiGuard`.
1a. Chấm hàng loạt: `AiGradingPort.requestBatch(teacher, submissionRefs)` kiểm quyền từng bài, tính một lần vào giới hạn tần suất, kiểm đủ credit cho cả lô (thiếu thì từ chối cả lô), tạo một dòng `ai_suggestions` cho mỗi bài và gửi việc `AI_TASK`; worker xử lý nền với số luồng giới hạn (BR-U13-20, 24).
2. Worker: lấy nội dung (U11, hoặc bản nộp tài liệu nhóm từ U14), văn bản phẳng + XML rút gọn (U09) hoặc code + kết quả test; rubric (U06) → prompt → kiểm JSON → `RubricPort.score` → `READY` (BR-U13-20…23).

## F3 - Kiểm lời giải mẫu
1. Giảng viên bấm "Kiểm lời giải mẫu" → việc `CODE_RUN` loại `VERIFY` chạy mọi test với cùng giới hạn như bài nộp.
2. Ghi kết quả kèm `contentHash` vào `questions.definition.verification` qua `QuestionVerificationPort` (U06); đạt hết test thì câu được coi là đã kiểm; `CodeLabCheckPort` cho U08 dùng khi duyệt (BR-U13-33).

## F3a - Chấm AI bài Practice của Student
1. Student bấm "Chấm với AI" trên một lượt `PRACTICE` Text/Diagram Essay đã nộp; U11 gọi `PracticeGradingPort`. U13 kiểm chủ attempt, dạng/chế độ bài, lượt chưa có kết quả hợp lệ, rồi giữ credit U07 với `purpose = PRACTICE_GRADING` tại lúc bấm.
2. Thiếu credit: trả "Không đủ credit AI", không gọi AI, không ghi `evaluations`; Student mua thêm rồi bấm lại được.
3. Đủ credit: tạo đúng một dòng `ai_suggestions` `PRACTICE_GRADING` cho attempt và việc AI. Worker dùng nội dung snapshot, rubric và XML Draw.io rút gọn nếu có; kiểm kết quả, tính điểm theo rubric, settle credit thực dùng rồi ghi điểm/phản hồi vào `evaluations` `kind = PRACTICE` qua `PracticeResultPort` (U15), chỉ Student đó xem.
4. Thử lại kỹ thuật dùng cùng dòng `ai_suggestions`; không tạo kết quả hoặc khoản trừ trùng. Lỗi cuối cùng, hoặc quá 5 phút chưa `READY` (`AiPendingSweeper` kiểm mỗi phút), chuyển `FAILED`, giải phóng credit và báo lỗi; Student bấm lại tạo dòng mới (index chỉ chặn hai dòng `QUEUED`/`RUNNING`/`READY` cùng attempt) (BR-U13-24).

## F4 - Chạy thử và chấm code
1. `TRY`: rate limit, chỉ test công khai, trả kết quả (BR-U13-34, 36).
2. `GRADE`: bài `GRADED` do U15 yêu cầu khi lượt nộp; bài `PRACTICE` do U11 yêu cầu. U13 chạy mọi test, tính điểm xác định và ghi `attempts.run_result` qua U11. `GRADED` gọi `CodeGradedPort.onGraded` của U15; `PRACTICE` gọi `PracticeResultPort` (BR-U13-35).
3. Judge0 lỗi → thử lại theo U03; hết lượt → `SANDBOX_ERROR`, U15 hiện "chưa chấm được" (BR-U13-31, 37).

## F5 - Quản trị AI
1. ADMIN sửa cấu hình, kill-switch (BR-U13-40).
2. Báo cáo AI Usage từ `ai_suggestions` (BR-U13-41).
