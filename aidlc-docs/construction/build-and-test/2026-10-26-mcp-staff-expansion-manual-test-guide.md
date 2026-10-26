# Kiểm thử MCP cho Teacher, Subject Manager và Admin

Trạng thái: các ca dưới đây **Chưa test trên tài khoản thật sau đợt mở rộng**. Test tự động và các kiểm tra Codex CLI trước đây được ghi riêng trong báo cáo, không thay thế các ca này.

## Chuẩn bị

1. Chạy backend/frontend từ mã mới bằng quy trình local thông thường; kiểm tra Flyway đã áp dụng `V20261026_1800`. Session triển khai này chưa restart hay migrate database đang dùng. Giữ dữ liệu và cấu hình provider hiện có.
2. Chuẩn bị tài khoản Teacher có lớp, Manager có môn phụ trách và Admin. Chọn lớp/môn QA và chỉ tạo nháp chưa phát hành. Không dùng bài/điểm đang công bố để thử ghi.
3. Teacher/Manager tạo token ở **Kết nối AI → Agent MCP**; Admin ở **Agent MCP** trên menu trái. Bắt đầu bằng READ_ONLY, sau đó tạo token READ_WRITE riêng cho ca tạo nháp/đề xuất.
4. Dùng cấu hình Codex CLI hiện có hoặc [hướng dẫn kết nối](../../../docs/guides/mcp-clients.md). Không gửi token/API key vào chat hay ghi vào Git. Kiểm tra handshake bằng `scripts/check-mcp.ps1`, rồi reconnect agent để cập nhật catalog.

## Danh sách nghiệm thu

| ID | Vai trò và thao tác | Kết quả mong đợi | Kết quả thật |
|---|---|---|---|
| S01 | Student mở Kết nối AI | Vẫn thấy và quản lý API cá nhân; không có MCP hoặc request mcp-credentials | Chưa test |
| S02 | Dùng token Student cũ gọi MCP | HTTP 401; không dùng được bất kỳ tool nào | Chưa test |
| R01 | Teacher/Manager/Admin dùng READ_ONLY; gọi capabilities/list | Chỉ tool của role; không có tool ghi; Admin chỉ 5 tool gồm capabilities | Smoke Teacher/READ_WRITE: capabilities + tools/list đạt; READ_ONLY chưa test |
| R02 | Teacher tìm lớp/môn và thử ID lớp khác | Chỉ lớp được dạy/môn được phép; ID ngoài quyền bị từ chối, không lộ nội dung | Smoke scoped discovery đạt; ID ngoài quyền chưa test |
| R03 | Manager tìm môn phụ trách, assignment/quiz/học liệu | Đúng scope; gửi cả classId và subjectId bị từ chối; page/size hợp lệ | Chưa test |
| M01 | Đọc học liệu dài, tiếp tục offset + expectedVersion | Ghép đủ nội dung, complete cuối cùng true; version đổi yêu cầu đọc lại | Chưa test |
| A01 | Teacher READ_WRITE yêu cầu agent tạo assignment QA qua create_assignment_draft | Trả assignmentId/proposalId/version/reviewUrl; bài DRAFT, chưa có nội dung áp dụng | Chưa test |
| A02 | Mở reviewUrl của A01, xem rồi áp dụng trên UI | Hiện đúng đề xuất đã lưu, không gọi provider/tạo đề xuất khác; chỉ áp dụng khi người dùng bấm | Chưa test |
| A03 | Retry A01 cùng requestRef/nội dung; sau đó đổi payload nhưng giữ ref | Lần đầu trả cùng ID, không thêm nháp; payload khác conflict | Chưa test |
| Q01 | Tạo quiz QA với settings, materials và questions hợp lệ | DRAFT + READY proposal; UI xem/chọn câu; chưa có câu active tự thêm vào quiz | MCP smoke đạt: DRAFT + READY, 2 câu; settings/material và thao tác UI còn chờ |
| Q02 | Tạo quiz với đáp án trùng/sai hoặc settings/tài liệu ngoài quyền | Từ chối, không để lại container/metadata retry nửa chừng | Chưa test |
| B01 | Manager tìm ngân hàng, đọc context với questionId rồi lưu BANK proposal | Đáp án/ngữ cảnh đúng môn; proposal xuất hiện trong luồng xem/duyệt ngân hàng | Chưa test |
| B02 | Teacher/Admin thử tool BANK; Manager ghép subjectId với questionId môn khác | Không thấy tool hoặc bị từ chối; không nhận câu/đáp án môn khác | Chưa test |
| G01 | Teacher tìm bài nộp và đọc grading context cá nhân/nhóm gốc | Có evaluationId/version, submissionRef/hash, rubricSnapshot, outputSchema và block ID; MEMBER bị từ chối | MCP list_assignments/list_submissions đạt; chưa có submission QA PENDING/DRAFT để đọc context |
| G02 | Đọc nhiều trang context; thử continuation cũ sau thay đổi snapshot | Ghép đủ trang cùng fingerprint; snapshot đổi bị từ chối, không nhận context cắt mà báo complete | Chưa test |
| G03 | Batch hỗn hợp bài hợp lệ, stale, ngoài quyền, duplicate và proposal sai | Mỗi item có outcome/code; bài hợp lệ giữ đề xuất; chưa lưu/công bố điểm | Chưa test |
| G04 | Retry batch với requestRef giữ nguyên; xem proposal trên UI | Không ghi trùng; trạng thái và nội dung đề xuất đúng, giáo viên tự duyệt | Chưa test |
| G05 | Đọc learning summary và distribution của lớp | Có source/calculatedAt/status và coverage; PARTIAL nếu vượt giới hạn; phân bố chỉ điểm đã công bố | Chưa test |
| D01 | Admin mở /admin/mcp và tạo/thu hồi token | Trang không yêu cầu personal AI; mặc định READ_ONLY; token chỉ hiển thị một lần | Chưa test |
| D02 | Admin đọc usage/quota/connections/platform summary | Báo cáo chỉ đọc; không có API key/token, bài nộp hoặc tool học vụ; dependency lỗi có trạng thái rõ | Chưa test |
| R04 | Thu hồi token, đổi role thành Student hoặc khóa tài khoản QA | Lời gọi kế tiếp không xác thực được; không cần xóa token khỏi DB | Chưa test |

## Gợi ý câu lệnh cho agent

- “Dùng MCP xem capabilities, tìm lớp QA tôi được dạy và liệt kê bài tập/quiz/học liệu. Chưa tạo hoặc sửa gì.”
- “Đọc read_authoring_requirements cho ASSIGNMENT. Tạo một assignment DRAFT tên QA MCP, requestRef riêng; trả reviewUrl để tôi xem trước, chưa áp dụng/phát hành.”
- “Liệt kê bài nộp của assignment QA, đọc đủ context theo continuation. Chỉ tạo đề xuất chấm; không lưu/công bố điểm.”
- “Với tài khoản Admin, tổng hợp sử dụng AI 7 ngày, quota và trạng thái kết nối/hệ thống. Không thay đổi cấu hình.”

## Khi có lỗi

- HTTP 401: kiểm tra token/expiry/revocation/current role/account status. Student không được dùng MCP.
- Tool không hiện: reconnect agent; kiểm tra role và READ_ONLY/READ_WRITE. Không tăng quyền học vụ cho Admin để chữa lỗi.
- MCP_RESULT_TOO_LARGE/MCP_BATCH_RESULT_LIMIT: giảm size hoặc số item, đọc từng context và continuation. MCP_CONTEXT_TOO_LARGE ở một block cần xem thủ công.
- SUBMISSION_CHANGED/STALE_PROPOSAL/version conflict: đọc lại context/version, soạn lại proposal. Không bỏ qua guard hoặc tự retry một thao tác công bố điểm.
- Ghi lại ID QA, tool, requestRef, mã lỗi và thời điểm. Không chụp token/API key hay sao chép nội dung bài thật vào báo cáo công khai.

Dọn nháp QA bằng UI sau nghiệm thu nếu không cần giữ. Chỉ dọn tài nguyên QA do mình tạo; không xóa bài/điểm thật để hoàn tác thử nghiệm.

## Runtime smoke test — 2026-10-26

- Backend và frontend local được restart từ checkout hiện tại; backend chạy profile `local,worker`, frontend chạy `npm run dev`.
- Flyway xác nhận đã áp dụng migration `20261026.1800 - create mcp write requests` lên database local.
- Qua `http://localhost:3000/mcp`: initialize, ping và tools/list đạt; server `ai-learning-platform`, protocol `2025-06-18`, có 23 tool.
- Teacher token hiện có xác thực thành công với `role=TEACHER`, `access=READ_WRITE`; `get_my_capabilities` khớp 23 tool.
- Các lệnh chỉ đọc `list_my_classes`, `list_my_subjects`, `list_assignments` đều thành công; thời điểm test trả 1 lớp, 1 môn và 2 bài tập trong lớp được trả về. Không ghi hay sửa dữ liệu.
- Các ca READ_ONLY, kiểm tra ID ngoài phạm vi, xem/áp dụng proposal trên UI, các ghi khác và các role khác vẫn chưa nghiệm thu runtime.

## Runtime test tạo quiz qua MCP — 2026-10-26

- Tool `create_quiz_draft` chạy bằng Teacher `READ_WRITE` trong lớp kiểm thử `U05-A` / môn Software Requirement.
- Quiz: **MCP QA - Software Requirement - 2026-10-26 182436**; `quizId=84c5f170-b3ab-43e4-9a73-dcc3c4468c3a`; [mở trang quiz](http://localhost:3000/quizzes/84c5f170-b3ab-43e4-9a73-dcc3c4468c3a).
- Kết quả: quiz `DRAFT`, version 1; `proposalId=6bf3e5d2-005a-44c4-8dd5-9e7f993837e8`, proposal `READY`, 2 câu trắc nghiệm mẫu.
- `list_quizzes` tìm thấy quiz mới. `read_quiz_context` xác nhận quiz chưa có câu active; đề xuất vẫn chờ người dùng xem/áp dụng trên UI.
- Gửi lại cùng `requestRef` và payload trả cùng quiz/proposal ID, không tạo bản thứ hai. Không gọi provider AI, không phát hành quiz, không gán cho người học.
- Còn chờ kiểm tra settings/material, mở proposal trên UI và thao tác người dùng chấp nhận câu hỏi.

## Runtime grading smoke test — 2026-10-26

- Teacher MCP `list_assignments` và `list_submissions` trả kết quả đúng scope ở lớp kiểm thử `U05-A`.
- Assignment **QA Gemini draft smoke test 2026-10-26** còn ở `DRAFT`; danh sách có một dòng `NOT_SUBMITTED`, không có evaluationId để đọc context.
- Assignment còn lại có một evaluation ở trạng thái `PUBLISHED`. Không đọc nội dung submission đó và không gửi proposal vào evaluation đã công bố.
- Backend chỉ cho lưu grading proposal khi evaluation là `PENDING` hoặc `DRAFT`. Vì không có bài QA đủ điều kiện, `read_grading_context` và `record_grading_proposals_batch` chưa được gọi; không có proposal hay điểm nào bị thay đổi.
- Để tiếp tục nghiệm thu an toàn, cần một submission QA tạo bởi tài khoản học sinh thử nghiệm, với evaluation ở trạng thái `PENDING`/`DRAFT`.
