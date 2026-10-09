# U13 AI & Code Execution - Business Logic Model

**Bản tài liệu 2026-10-09**: không primary UC; phần chạy AI và Judge0 của UC 25, 29, 38, 43, nhóm AI của Settings UC 70–71, luồng phụ AI soạn đề (UC 35, 42–45, 57) và tóm tắt/embedding từ màn xem học liệu (UC 15, 34, 55) theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-AIG-001, US-AIG-002, US-AIG-003. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## Phân vai theo UC

| UC | Màn (unit chủ trì) | Phần của U13 |
|---|---|---|
| 25 Complete Code Lab | Codelab Workspace (U11) | `TRY` chạy test công khai qua Judge0; `GRADE` chấm khi nộp (F4) |
| 29 Grade Practice Assignment | Submission History (U11) | Giữ credit, chạy AI chấm theo rubric, ghi kết quả qua U15 (F3a) |
| 38 Grade Submission With AI | Panel AI đề xuất trong Grading Workspace, chấm hàng loạt trên Student Submissions (U15) | Đề xuất chấm từng bài hoặc theo lô (F2) |
| 43 Create/Update/Delete Code Lab | Assignment Form (U08, U09) | Kiểm lời giải mẫu, lưu theo `contentHash`, trả lời `CodeLabCheckPort` (F3) |
| 70–71 View Settings, Edit Setting | Setting List, Setting Detail (U03) | Khai báo và dùng mục nhóm AI (F5) |
| 35, 42–45, 57 (luồng phụ) | Quiz Detail, Assignment Form (U08, U09), Question List (U06) | AI soạn câu hoặc khung (F1) |
| 15, 34, 55 (luồng phụ) | View Material (U05) | Giữ credit khi bấm Tóm tắt tài liệu, ghi nhận summary/embedding (F6) |

## F1 - Tạo đề xuất câu hỏi hoặc khung
1. Nhận yêu cầu: U06 (Question List, câu ngân hàng, R2) qua API U13; U08 (quiz, Text Essay, Code Lab của lớp hoặc của môn) và U09 (khung `SKELETON_DRAFT` Diagram Essay, bài nhóm) qua `AiDraftPort`. Kiểm vai trò (từ chối `ADMIN`, `STUDENT`), phạm vi R2/R3/R4, đích `DRAFT` và tham số (BR-U13-03, 10…13).
2. `AiGuard`: kill-switch, trần ngày, tần suất (đọc Settings qua `SettingsPort`), `CreditPort.reserve` ước tính (BR-U13-03).
3. Tạo dòng `ai_suggestions` `QUEUED` (`credit_status = RESERVED`) + việc `AI_TASK`.
4. Worker: `RagRetrievalPort.retrieve(scope, query, k, requesterId)` (phạm vi lớp: học liệu môn + lớp; phạm vi môn: chỉ học liệu môn) tính credit embedding của người yêu cầu (U05 gọi `AiUsagePort`, một dòng riêng) → dựng prompt (khối dữ liệu phân cách) → `AiGateway.generate(task, model, prompt, jsonSchema)` → kiểm JSON + quy tắc U06 hoặc `validateSkeleton` → `READY`; `settle` credit theo token thật; ghi token, chi phí, độ trễ (BR-U13-04…09, 14). Truy xuất bị từ chối trước khi gọi Gemini thì `release` phần credit tạo nội dung đã giữ.
5. Người soạn chọn câu hoặc xác nhận khung → đích lưu nháp (U06 câu ngân hàng, U08 câu riêng của bài, U09 thay khung) → `ACCEPTED`; bỏ → `DISCARDED` (BR-U13-15).

## F2 - Đề xuất chấm (UC 38)
1. U15 gọi `AiGradingPort.request(teacher, submissionRef)` (bài cá nhân, hoặc tài liệu chung của bài nhóm); U13 kiểm giảng viên chính của lớp chứa bài nộp (bài của môn: lớp của sinh viên), bài `GRADED` Text/Diagram Essay hoặc bài nhóm, `AiGuard`.
2. Chấm hàng loạt: `AiGradingPort.requestBatch(teacher, submissionRefs)` kiểm quyền từng bài, tính một lần vào giới hạn tần suất, kiểm đủ credit cho cả lô (thiếu thì từ chối cả lô), tạo một dòng `ai_suggestions` cho mỗi bài và gửi việc `AI_TASK`; worker xử lý nền với số luồng giới hạn (BR-U13-20, 24).
3. Worker: lấy nội dung (U11, hoặc bản nộp tài liệu nhóm từ U14), văn bản phẳng + XML rút gọn (U09); rubric đã khóa (U06) → prompt → kiểm JSON → `RubricPort.score` → `READY`, `settle` (BR-U13-21…23). Panel AI đề xuất của U15 trong Grading Workspace hiện kết quả; giảng viên bấm "Dùng đề xuất" thì U15 điền sẵn checklist.

## F3 - Kiểm lời giải mẫu (UC 43)
1. Trên Assignment Form, `CodeLabCheckPanel` (U09) hiện trạng thái từng câu `CODE` qua `CodeLabCheckPort.statusOf(assignmentId)`.
2. Người soạn bấm "Kiểm lời giải mẫu" (`VerifySolutionButton`) → `POST /api/v1/code-runs` `VERIFY` với `assignmentId`, `questionId`. U13 kiểm R3/R4 (bài của lớp) hoặc R2 (bài của môn), bài `DRAFT`, câu thuộc bài (`AssignmentQueryPort`).
3. Đọc phiên bản câu đầy đủ (`BankQueryPort.getVersion`), tính `contentHash`; đã có dòng `VERIFY` `DONE` cùng hash thì trả luôn, đang có dòng `QUEUED`/`RUNNING` cùng hash thì trả dòng đó; ngược lại tạo dòng `code_runs` `VERIFY` `QUEUED` + việc `CODE_RUN`; audit.
4. Worker chạy lời giải mẫu với mọi test, cùng giới hạn như bài nộp; ghi `results`, `passed_all`, `DONE` (hoặc `SANDBOX_ERROR`).
5. U08 khi duyệt gọi `CodeLabCheckPort.check(assignmentId)`: câu chưa kiểm, không đạt hoặc hash cũ → lỗi duyệt (BR-U13-33).

## F3a - Chấm AI bài Practice của Student (UC 29)
1. Student bấm "Chấm với AI" trên Submission History (nút của U11) cho một lượt `PRACTICE` Text/Diagram Essay đã nộp; U11 gọi `PracticeGradingPort.request`. U13 kiểm chủ attempt, dạng/chế độ bài, lượt chưa có kết quả hợp lệ, `AiGuard`, rồi giữ credit U07 với `purpose = PRACTICE_GRADING` tại lúc bấm.
2. Thiếu credit: "Không đủ credit AI", không gọi AI, không ghi `evaluations`; Student mua thêm (My Credit Package, Public Credit Packages) rồi bấm lại được.
3. Đủ credit: tạo đúng một dòng `ai_suggestions` `PRACTICE_GRADING` cho attempt và việc AI. Worker dùng nội dung snapshot, rubric và XML Draw.io rút gọn nếu có; kiểm kết quả, tính điểm theo rubric, `settle` credit thực dùng rồi ghi điểm/phản hồi vào `evaluations` `kind = PRACTICE` qua `PracticeResultPort` (U15), chỉ Student đó xem.
4. Thử lại kỹ thuật dùng cùng dòng; không tạo kết quả hoặc khoản trừ trùng. Lỗi cuối cùng, hoặc quá 5 phút chưa `READY` (`AiPendingSweeper` kiểm mỗi phút), chuyển `FAILED`, chốt lượng dùng thật đã ghi và trả phần dư (chưa dùng mới hoàn toàn bộ), báo lỗi; Student bấm lại tạo dòng mới (BR-U13-24).

## F4 - Chạy thử và chấm code (UC 25)
1. `TRY`: Student trên Codelab Workspace; kiểm chủ lượt đang làm, rate limit 5/phút; chạy đồng bộ chỉ test công khai, trả kết quả và ghi bản mới nhất vào `attempts.run_result` (BR-U13-34, 36).
2. `GRADE`: bài `GRADED` do U15 yêu cầu khi lượt nộp; bài `PRACTICE` do U11 yêu cầu. U13 tạo dòng `code_runs` `GRADE` (idempotent theo `attempt_id`), worker chạy mọi test, tính điểm xác định, ghi `attempts.run_result` qua U11. `GRADED` gọi `CodeGradedPort.onGraded` của U15; `PRACTICE` gọi `PracticeResultPort.record` (BR-U13-35).
3. Judge0 lỗi → thử lại theo U03; hết lượt → `SANDBOX_ERROR`, U15 hiện "chưa chấm được"; giảng viên bấm chấm lại → `CodeRunPort.regrade` (BR-U13-31, 37).

## F5 - Cài đặt AI và giám sát (UC 70–71, UC 58)
1. Khi khởi động, `AiSettingDefinitions` đăng ký các mục nhóm AI với `SettingDefinitionRegistry` của U03; mục chưa có được tạo với giá trị mặc định (BR-U03-84).
2. Admin xem và sửa trên Setting List/Setting Detail của U03; U03 kiểm `ADMIN`, giới hạn theo khai báo, version, audit (BR-U13-40).
3. `AiGuard`, `AiTaskHandler` đọc giá trị qua `SettingsPort` (cache 30 giây); giá trị mới áp cho yêu cầu kế tiếp.
4. U16 hiển thị số liệu AI trên Admin Dashboard qua `AiUsageStatsPort.summarize(from, to)` gộp từ `ai_suggestions` (BR-U13-41).

## F6 - Credit khi người xem yêu cầu tóm tắt (U05)
1. Upload/trích chữ không gọi U13. View Material gọi quote(requester) qua U05 sau kiểm quyền xem.
2. U05 khóa lesson EXTRACTED chưa yêu cầu, kiểm quyền xem/AI guard, gọi hold cùng transaction ghi summary_requested_by/at và enqueue MATERIAL_SUMMARY. Student/Teacher/Subject Manager trong scope đều hợp lệ; thiếu credit rollback yêu cầu, giữ lesson. Hai người bấm chỉ một payer/HOLD.
3. Worker dùng requester đã lưu và HOLD, begin theo chunk/merge/embedding: RUN gọi provider, REPLAY checkpoint, BUSY chờ; child calls không reserve thêm. U05 kiểm claim/hạn và U13 kiểm ticket cùng transaction complete/fail; không đọc repository chung.
4. INDEXED/FAILED/hết hạn chốt lượng dùng thật, trả dư; embedding lỗi giữ summary. Giai đoạn AI có deadline 24 giờ từ nhận yêu cầu, HOLD 25 giờ từ nhận yêu cầu là dự phòng. Giai đoạn trích chữ trước yêu cầu không có HOLD.
5. Người xem khác dùng summary/trạng thái có sẵn; không giữ credit của người poll hoặc người bấm trùng. Student không được truy xuất RAG, soạn đề hoặc gọi EMBEDDING độc lập.

## Checkpoint học liệu và hold

Hold MATERIAL_SUMMARY là dòng `lessonId:HOLD`. Mỗi call có requestRef chunkIndex/MERGE/EMBEDDING riêng và hold_id; complete ghi result cấu trúc cùng lượng dùng, cộng vào hold một lần. Worker U05 tra hold theo lesson, đọc checkpoint hoàn tất trước gọi provider. Release terminal settle lượng thật, trả dư; scan U05 hết hạn tuyệt đối 24 giờ, scanner hold U13 25 giờ chỉ dự phòng. Embedding thất bại không xóa summary. Thiết kế này không bảo đảm exactly-once gọi provider khi tiến trình chết giữa phản hồi provider và commit.
