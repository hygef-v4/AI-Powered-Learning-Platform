# Hướng dẫn tự test MCP và AI

Ngày 2026-10-26. Chrome vẫn giữ phiên student và hộp thoại bàn giao token MCP; phiên teacher đã mở riêng trong in-app browser và đang ở bài nháp QA bên dưới. Giữ nguyên khóa Gemini đang có. Chỉ dùng bài riêng có nhãn QA; không chốt/công bố điểm để test.

Bài test tạo đề: [QA Gemini draft smoke test 2026-10-26](http://localhost:3000/assignments/6fde44e6-05f1-4321-8d21-659ce2879c31?classId=4d18e255-1789-45d6-8fbb-351e2188f672). Đây là bản nháp mới trong U05-A, chưa xếp lịch/công bố.

## 1. Kiểm tra kết nối

1. Mở `http://localhost:3000`, đăng nhập tài khoản cần test. Vào **Kết nối AI**.
2. Teacher: Gemini phải hiện đã cấu hình, model đang lưu và kết quả kiểm tra gần nhất. Bấm **Kiểm tra** nếu muốn xác nhận kết nối hiện tại; thao tác gửi yêu cầu nhỏ tới nhà cung cấp. Mong đợi **Đạt** cùng thời điểm mới. Nếu lỗi, giữ nguyên khóa/model, ghi thông báo an toàn và thời điểm.
3. Student: hiện chưa có khóa. Muốn dùng nút phản hồi AI Practice phải thêm khóa cá nhân student; không sao chép khóa teacher để lấp cấu hình. Có thể bỏ qua ca Practice nếu chưa muốn thêm khóa.

## 2. MCP chỉ đọc trước

1. Trong tài khoản muốn dùng agent, chọn **Agent MCP → Kết nối agent mới**, nhãn `MCP-AI-QA-read`, quyền **Chỉ đọc**, hạn 7 ngày. Tự bấm tạo và lưu token riêng; không gửi token trong chat.
2. Từ PowerShell ở thư mục repository chạy:

   ```powershell
   .\scripts\check-mcp.ps1
   ```

   Nhập token tại lời nhắc ẩn. Mong đợi `Server = ai-learning-platform`, protocol được hỗ trợ, danh sách tool đúng role. Nếu lỗi, thử `-Endpoint http://localhost:8080/mcp` để phân biệt proxy và API; không bỏ xác thực.
3. Đăng ký server riêng `nen-tang-hoc-tap` trong agent theo [hướng dẫn MCP](../../../docs/guides/mcp-clients.md). Không dùng API key Gemini làm token MCP, không sửa mục Unity. Khởi động lại đúng agent. Với Codex, kiểm tra `/mcp`; với Antigravity, kiểm tra danh sách MCP/công cụ của agent.
4. Lấy ID bài test đang tồn tại từ trang bài tập. Gửi prompt: **Dùng server nen-tang-hoc-tap, gọi read_assignment_context với assignmentId [ID bài test]. Chỉ đọc và báo lại tên bài; không tạo hoặc sửa dữ liệu.** Mong đợi đúng bài và phạm vi dữ liệu của role. Student không được nhận nội dung đề/rubric bị hạn chế chỉ vì có token.
5. Kiểm tra tool ghi không có trong danh sách khi token chỉ đọc. Muốn test từ chối truy cập, dùng ID bài test khác ngoài quyền đã biết; mong đợi lỗi không có quyền, không lộ dữ liệu.
6. Thu hồi token test bằng UI nếu không dùng tiếp. Chạy lại diagnostic bằng token đó: mong đợi HTTP 401. Chỉ thu hồi token test vừa tạo.

## 3. MCP đọc và ghi (tùy chọn)

1. Tạo token test riêng quyền **Đọc và ghi** và cập nhật đúng agent. Đọc `tools/list` để lấy schema thực tế.
2. Teacher/manager: nhờ agent đọc bài test rồi dùng `save_assignment_draft` với version vừa đọc, tạo đề xuất nháp. Mong đợi UI hiện đề xuất chờ áp dụng; nội dung bài đang lưu chưa bị tự ghi đè.
3. Nếu test quiz, dùng `read_quiz_context` rồi `save_quiz_draft` trên quiz nháp riêng. Mong đợi đề xuất đúng quiz, không tự công bố.
4. Nếu test chấm, dùng `read_authorized_submission` rồi `record_grading_proposal` trên bài nộp test. Yêu cầu agent lấy đúng submission reference/schema và rubric từ dữ liệu đọc được. Mong đợi đề xuất xuất hiện, điểm đã lưu không đổi. Không chốt/công bố.
5. Nếu test student, dùng `read_own_work` và `record_private_feedback` trên Practice của mình, giữ submissionHash vừa đọc. Mong đợi phản hồi riêng. Đây là agent tự tạo nội dung; khác với nút AI dùng API cá nhân trong mục 4.

## 4. Nút AI trong ứng dụng

| Ca | Thao tác | Mong đợi |
|---|---|---|
| AI-01 Tạo đề | Teacher tạo bài nháp test → Tạo nháp bằng AI → API cá nhân/Gemini, yêu cầu 3 phần, tổng 10 điểm | Có đề xuất READY; số phần/rubric hợp lý, tổng tối đa đúng 10; chỉ áp dụng trên bài test |
| AI-02 Quiz | Teacher tạo quiz nháp test → tạo câu hỏi AI với nguồn Gemini cá nhân | Có đề xuất câu hỏi/đáp án hợp lệ, đúng số lượng yêu cầu; chưa công bố |
| AI-03 Chấm | Mở bài nộp test đủ đề/rubric → Yêu cầu đề xuất mới một lần | Đến trạng thái thành công hoặc lỗi rõ ràng; READY có điểm từng mục, minh chứng/lý do/cách cải thiện; điểm đã lưu chưa đổi |
| AI-04 Áp dụng | Trên bài test, áp dụng đề xuất vào bản nháp | Chỉ cập nhật biểu mẫu; tổng đúng tổng các mục; sửa 0.5 cho mục tối đa 1 được, vượt 1 bị chặn; lưu nháp/tải lại giữ điểm và feedback |
| AI-05 Practice | Student có API cá nhân, nộp Practice test rồi yêu cầu phản hồi | Có phản hồi riêng đúng bài, không phải điểm Graded đã công bố |
| AI-06 Nguồn trường | Mở lựa chọn nguồn khi chưa cấu hình shared | Không cho dùng nguồn trường chưa sẵn sàng; không tự chuyển sang khóa người khác |
| AI-07 Tải lại | Trong lúc AI đang xử lý, tải lại trang bài test | Theo dõi lại task, không mắc kẹt vô hạn; lỗi có thông báo, đề xuất thành công đọc được |

Với ca thất bại: ghi giờ bắt đầu/kết thúc, role, URL/ID bài test, nguồn/provider, task/proposal ID nếu thấy, thông báo UI và ảnh. Không gửi key, bearer token, cookie hoặc toàn bộ header request. Không bấm lặp nhiều lần khi task đang chạy.

## Ghi nhận kết quả

- MCP diagnostic: Đạt với token thật, Codex CLI nhận diện `ai-learning-platform`, protocol `2025-06-18`, ba tool chỉ đọc.
- Agent đọc dữ liệu: Đạt. Codex CLI gọi `read_authorized_material` thành công với học liệu lớp hiện tại.
- AI-01 tạo đề: Đạt trên bản nháp QA. Lần đầu AI tạo rubric sai tổng (11.33/10), UI chặn áp dụng; lần hai đạt 10.00/10 với 3 câu, 3/3 phần có rubric và 9 tiêu chí.
- AI-04 áp dụng/lưu/tải lại nội dung rubric: Đạt trên bản nháp QA. Sau khi áp dụng, khôi phục tên QA; tải lại vẫn có 3 câu, 9 tiêu chí, tổng 10 điểm. Chưa test nhập 0.5 thủ công hoặc chặn vượt giới hạn.
- Teacher Gemini connection check: Đạt; khóa/model hiện tại được giữ nguyên.
- Teacher MCP/Codex CLI: Đạt. Codex dùng token teacher qua biến môi trường, nhìn thấy 7 tool và gọi read_assignment_context trên bài QA thành công. Chỉ đọc; không gọi tool ghi. Token có quyền đọc/ghi, nên nếu chỉ cần đọc thì lần sau có thể tạo token Chỉ đọc riêng.
- Chưa test: AI-02 Quiz, AI-03 đề xuất chấm trên một bài nộp test, AI-05 Practice, AI-06 nguồn trường, AI-07 khôi phục task sau tải lại, và nhập điểm 0.5/chặn vượt giới hạn. Không dùng điểm đã công bố làm dữ liệu thử.

Token student `Codex CLI test read-only` còn hạn đến 02/11/2026; secret không được lưu trong Codex config. Token teacher `teacher-mcp` có quyền Đọc và ghi, còn hạn đến 25/11/2026. Theo yêu cầu của user, token đã được lưu ở Windows user environment variable `LEARNING_PLATFORM_MCP_TOKEN`; Codex config chỉ lưu tên biến, không lưu token. Clipboard đã được xóa sau kiểm thử. Codex đang mở cần thoát và khởi chạy lại trong PowerShell mới:

```powershell
$env:LEARNING_PLATFORM_MCP_TOKEN = [Environment]::GetEnvironmentVariable('LEARNING_PLATFORM_MCP_TOKEN', 'User')
codex
```

Codex nhận token từ biến môi trường khi khởi chạy; phiên đang mở sẽ không tự nhận giá trị mới. Để gỡ biến user sau khi thử, chạy `[Environment]::SetEnvironmentVariable('LEARNING_PLATFORM_MCP_TOKEN', $null, 'User')`; với cửa sổ hiện tại, chạy `Remove-Item Env:LEARNING_PLATFORM_MCP_TOKEN`. Vì token teacher có quyền ghi và đã được gửi trong cuộc trò chuyện, hãy cân nhắc thu hồi token này rồi tạo token thay thế trực tiếp trong UI; không gửi secret mới trong chat.
