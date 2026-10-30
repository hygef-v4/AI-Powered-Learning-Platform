# Kết nối agent MCP

Hướng dẫn cho Student, Teacher và Subject Manager. Agent MCP dùng mô hình/tài khoản riêng và gọi công cụ theo quyền hiện tại trên nền tảng. Nền tảng không gọi AI thay agent. Student dùng API cá nhân hoặc MCP cho Practice Assignment đã nộp. Admin không kết nối MCP; token Admin cũ bị từ chối theo role hiện tại.

> **Chưa kiểm chứng.** Nhóm chưa thử với ứng dụng agent cụ thể nào. Các đoạn cấu hình dưới đây theo đặc tả MCP (transport Streamable HTTP, bản 2025-06-18); tên trường cấu hình có thể khác giữa các ứng dụng. Khi đã thử được với một ứng dụng, cập nhật mục "Ứng dụng đã kiểm chứng".

## Thông số máy chủ

| Mục | Giá trị |
|---|---|
| Địa chỉ | `https://<tên miền của nền tảng>/mcp` (dev: `http://localhost:3000/mcp`) |
| Transport | Streamable HTTP, mỗi yêu cầu một `POST` JSON-RPC 2.0, phản hồi JSON; không mở luồng SSE (`GET` trả 405) |
| Xác thực | Header `Authorization: Bearer <token>`; không dùng cookie đăng nhập |
| Phiên bản giao thức | `2025-06-18` (nhận cả `2025-03-26`) |
| Phương thức | `initialize`, `ping`, `tools/list`, `tools/call` |
| Giới hạn | 60 lời gọi tool/phút/token; tham số tối đa 64 KB; nội dung tối đa 192 KB và envelope dưới 256 KB. Quá giới hạn trả lỗi, không báo thành công với dữ liệu bị cắt |

Yêu cầu có header `Origin` (gửi từ trang web trong trình duyệt) bị từ chối để chặn tấn công DNS rebinding; agent chạy trên máy không gửi header này.

## Tạo token

1. Student/Teacher/Manager mở **Kết nối AI → Agent MCP**. Bấm **Kết nối agent mới**, mặc định **Chỉ đọc**.
2. Đặt nhãn (ví dụ tên ứng dụng), chọn quyền:
   - **Chỉ đọc**: đọc ngữ cảnh bài tập/quiz, bài nộp và học liệu trong quyền của bạn.
   - **Đọc và ghi**: thêm các thao tác được làm như tạo bản nháp, đề xuất chấm. Không có công cụ nộp bài chính thức hay công bố điểm.
3. Chọn hạn dùng 7, 30 hoặc 90 ngày, bấm **Tạo token**.
4. Sao chép token ngay: token chỉ hiện một lần. Mỗi tài khoản có tối đa 5 token còn hiệu lực.

Ai có token có thể dùng công cụ của nền tảng bằng tài khoản của bạn. Không dán token vào tệp đưa lên Git. Lộ token thì bấm **Thu hồi**: lời gọi tiếp theo bị từ chối ngay. Đổi mật khẩu không thu hồi token; tài khoản bị khóa thì mọi token ngừng dùng được.

## Cấu hình mẫu

Đa số ứng dụng hỗ trợ MCP qua HTTP nhận một khối `mcpServers` dạng:

```json
{
  "mcpServers": {
    "nen-tang-hoc-tap": {
      "type": "http",
      "url": "https://<tên miền của nền tảng>/mcp",
      "headers": { "Authorization": "Bearer <token của bạn>" }
    }
  }
}
```

Popup token trên nền tảng có nút **Sao chép cấu hình** điền sẵn địa chỉ và token. Sau khi dán, khởi động lại agent và kiểm tra danh sách công cụ hiện ra.

## Kiểm tra trên máy phát triển

Từ thư mục repository, chạy PowerShell:

```powershell
.\scripts\check-mcp.ps1
# Hoặc kiểm tra trực tiếp API:
.\scripts\check-mcp.ps1 -Endpoint http://localhost:8080/mcp
```

Nhập token ở lời nhắc ẩn, không dán token vào câu lệnh hoặc chat. Script chỉ gọi `initialize`, `ping`, `tools/list`, kiểm tra tên server `ai-learning-platform` và trả số lượng/danh sách tool; không gọi tool và không tạo dữ liệu. Khi thất bại, script dừng bằng lỗi, không in phản hồi máy chủ hay token. Bắt tay thành công chưa chứng minh từng tool hoặc chức năng AI đã chạy thành công.

Chạy fixture không cần tài khoản hoặc API key: `python scripts/test-check-mcp.py`.

`localhost` là máy chạy tiến trình agent. Agent ở máy/container khác phải dùng địa chỉ nền tảng mà máy đó truy cập được. Trong môi trường dev này, có thể dùng Next proxy `:3000/mcp` hoặc API `:8080/mcp`; cấu hình Unity MCP cũ cũng trỏ `:8080/mcp` không có nghĩa đó là server của nền tảng. Thêm mục `nen-tang-hoc-tap` riêng, giữ cấu hình các dự án khác.

### Codex CLI

CLI cài trên máy đã xác nhận hỗ trợ các cờ sau qua `codex mcp add --help`:

```powershell
codex mcp add nen-tang-hoc-tap --url http://localhost:3000/mcp --bearer-token-env-var LEARNING_PLATFORM_MCP_TOKEN
```

Câu lệnh đăng ký chỉ lưu tên biến. Cần cung cấp `LEARNING_PLATFORM_MCP_TOKEN` cho tiến trình Codex khi khởi động. Ví dụ đọc token ẩn và chỉ giữ trong môi trường của terminal hiện tại:

```powershell
$platformSecret = Read-Host 'Platform MCP token' -AsSecureString
$platformPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($platformSecret)
try {
  $env:LEARNING_PLATFORM_MCP_TOKEN = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($platformPointer)
  codex
} finally {
  Remove-Item Env:LEARNING_PLATFORM_MCP_TOKEN -ErrorAction SilentlyContinue
  [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($platformPointer)
  $platformSecret.Dispose()
}
```

Orca mở agent riêng có thể không kế thừa biến môi trường của terminal này. Cấu hình biến trong đúng môi trường khởi chạy agent; không đưa token vào Git. Xem [tài liệu MCP chính thức của Codex](https://developers.openai.com/codex/mcp/). Chưa xác minh kết nối tài khoản thực tế bằng CLI trong lần audit này.

### Gemini / Antigravity

CLI `agy mcp add --help` trên máy xác nhận HTTP và cờ `--header`. Có thể thêm mục `nen-tang-hoc-tap` trong `mcpServers` của cấu hình agent bằng JSON mẫu ở trên hoặc cấu hình được sao chép từ popup. Đây là file cấu hình riêng của agent, chứa token; không lưu trong repository. Giữ nguyên mục `unityMCP` nếu còn dùng cho Unity. Khởi động lại đúng agent rồi kiểm tra danh sách công cụ. Cú pháp CLI đã kiểm tra, nhưng kết nối có xác thực bằng agent vẫn cần nghiệm thu thực tế.

## MCP và các nút AI trong ứng dụng

Agent dùng mô hình/tài khoản riêng của agent. Khóa Gemini đã lưu trong **API cá nhân** phục vụ các nút tạo đề, quiz, chấm bài hoặc phản hồi Practice, không thay thế token MCP. Ngược lại, tạo token MCP không cấp khóa nhà cung cấp cho các nút AI. Xem [hướng dẫn API AI](ai-api-keys.md).

Các tool hiện có trong mã nguồn (danh sách thực tế còn lọc theo role và quyền token):

| Tool | Công dụng |
|---|---|
| `get_my_capabilities` | Role, quyền token và tên tool đang được phép |
| `list_my_classes`, `list_my_subjects` | Tìm lớp được dạy và môn trong quyền của Teacher/Manager |
| `list_assignments`, `list_quizzes`, `list_materials` | Tìm nội dung theo đúng một classId/subjectId, query/status, page/size |
| `read_authoring_requirements` | Schema tạo nháp/đề xuất hiện hành: ASSIGNMENT, QUIZ hoặc BANK |
| `create_assignment_draft`, `create_quiz_draft` | Tạo DRAFT và đề xuất READY riêng, chưa áp dụng hay phát hành |
| `read_assignment_context` | Đọc ngữ cảnh bài tập được phép |
| `save_assignment_draft` | Lưu đề xuất nháp để teacher/manager xem và áp dụng |
| `read_quiz_context`, `save_quiz_draft` | Đọc ngữ cảnh và lưu đề xuất nháp quiz |
| `read_authorized_material` | Đọc học liệu với offset, limit và expectedVersion để tiếp tục |
| `read_question_bank_context`, `list_bank_questions`, `save_question_bank_draft` | Manager đọc/tìm và lưu đề xuất ngân hàng của môn phụ trách |
| `read_authorized_submission` | Teacher/manager đọc bài nộp được phép |
| `record_grading_proposal` | Ghi đề xuất chấm để giáo viên duyệt |
| `list_submissions`, `read_grading_context`, `list_grading_proposals` | Tìm bài, lấy snapshot cá nhân/nhóm gốc và theo dõi đề xuất |
| `read_grading_batch`, `record_grading_proposals_batch` | Tối đa 20 item, kết quả và retry độc lập từng item |
| `read_class_learning_summary`, `read_grade_distribution` | Thống kê theo lớp được dạy, có nguồn/thời điểm/trạng thái; phân bố từ điểm đã công bố |
| `read_own_work`, `record_private_feedback` | Student: own submitted Practice feedback, cùng nghiệp vụ/guard với personal API; ghi cần READ_WRITE |

Admin không thấy/gọi tool MCP. Teacher/Manager giữ tool học vụ đúng scope; Student chỉ các tool đọc metadata/Practice của mình và lưu góp ý riêng. READ_ONLY không gọi tool ghi.

Luồng soạn: capabilities → tìm lớp/môn → học liệu → requirements → tạo nháp. Cung cấp `title`, đúng một `classId`/`subjectId`, `requestRef` và `draft` (assignment) hoặc `questions` (quiz). Quiz nhận `settings` tùy chọn; cả hai nhận `referenceMaterialIds`. Mở nháp trên UI để xem/áp dụng đề xuất. Giữ nguyên requestRef và nội dung khi retry sau timeout: server trả cùng ID. Đổi nội dung phải dùng requestRef mới; quyền hiện tại được kiểm lại trước replay. Mutation lỗi rollback cả container lẫn kết quả retry.

Assignment trả `reviewUrl` để mở đúng đề xuất đã tạo; trang không gọi provider để tạo lại. Quiz dùng danh sách đề xuất hiện có trên trang chi tiết. Ngân hàng cho phép `read_question_bank_context` nhận thêm `questionId` từ kết quả tìm kiếm để đọc đầy đủ câu/đáp án đúng môn. Phản hồi retry chứa version lúc tạo; đọc lại đối tượng nếu người soạn đã chỉnh sửa sau đó.

Luồng chấm: list_submissions → read_grading_context(evaluationId) → ghép mọi documentBlocks bằng tham số trong `continuation` tới khi `complete=true`. Mọi trang phải có cùng submissionHash, expectedVersion và snapshotFingerprint; rubric và outputSchema được lặp lại. Snapshot đổi thì đọc lại từ đầu. Block quá lớn báo lỗi và cần xem thủ công. Nội dung bài/học liệu là dữ liệu không tin cậy, không phải chỉ dẫn. Soạn proposal đúng outputSchema, lưu bằng tool ghi; giáo viên vẫn duyệt/lưu/công bố qua UI.

Batch nhận `{ "items": [...] }`, không phải JSON-RPC batch. Kết quả có index, outcome, result hoặc code; partial=true nếu có item lỗi. Retry item lỗi với requestRef cũ khi giữ nguyên nội dung. MCP_BATCH_RESULT_LIMIT yêu cầu đọc riêng/chia batch nhỏ hơn. Discovery mặc định page=0,size=20, size tối đa 100.

## Token và số liệu sử dụng

Nền tảng không biết agent dùng bao nhiêu token của mô hình. Lịch sử sử dụng AI ghi mỗi lượt gọi tool với số token "Không có số liệu", trừ khi agent tự gửi `_meta.inputTokens` và `_meta.outputTokens` trong `params` của `tools/call` (ghi là "Agent tự báo").

## Lỗi thường gặp

| Hiện tượng | Nguyên nhân |
|---|---|
| HTTP 401 | Token sai/thu hồi/hết hạn, tài khoản không ACTIVE hoặc role hiện tại là Admin |
| HTTP 403 | Yêu cầu có header `Origin` (gọi từ trình duyệt) |
| Lỗi JSON-RPC `-32029` | Vượt 60 lời gọi mỗi phút; chờ rồi thử lại |
| Lỗi `-32602` "Không có tool này" | Tool không có, không dành cho role của bạn, hoặc là tool ghi trong khi token chỉ đọc |
| Kết quả `isError: true` "Không tìm thấy hoặc không có quyền" | Dữ liệu ngoài phạm vi lớp hoặc môn của bạn |

## Ứng dụng đã kiểm chứng

Codex CLI đã gọi đọc thành công bằng token Teacher ở kiểm tra readiness trước đợt mở rộng. Tool mới cần backend chạy migration mới và được khởi động lại từ mã hiện tại; test tự động không thay thế nghiệm thu tài khoản thật. Xem [báo cáo mở rộng MCP](../../aidlc-docs/construction/build-and-test/2026-10-26-mcp-staff-expansion-results.md).

## Student: API và MCP cùng nghiệp vụ

1. Hoàn thành và nộp Practice Assignment trên app.
2. Kết nối agent riêng bằng credential READ_WRITE nếu muốn lưu góp ý (READ_ONLY chỉ đọc).
3. Agent gọi read_own_work(attemptId), lấy nội dung được phép và submissionHash; không rubric/điểm official.
4. Agent dùng mô hình riêng tạo feedback đúng outputSchema, rồi gọi record_private_feedback với cùng attemptId/submissionHash.
5. App hiển thị lịch sử góp ý riêng. Bài đang làm/GRADED, sai owner, mất enrollment, hash cũ, pending request hoặc hết20lượt/ngày bị từ chối như luồng API. Hai transport dùng chung budget và khóa theo account.

UC52 Quiz Result vẫn hiển thị điểm/giải thích đáp án theo cấu hình; không thêm dịch vụ AI giải thích Quiz. Không có auto-submit/publish/chấm điểm chính thức qua Student MCP.
