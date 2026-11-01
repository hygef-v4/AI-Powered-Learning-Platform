# Skill và hook cho agent MCP

## Giao việc trong agent ngoài, duyệt trên hệ thống

Người dùng giao việc và trả lời câu hỏi trong Codex, Claude Code, OpenCode hoặc Antigravity. Không bắt buộc điền form trên nền tảng trước. get_my_capabilities trả quyền và hướng dẫn luồng; read_authoring_requirements bổ sung schema/cách hỏi làm rõ, ngay cả khi chưa cài skill.

Nếu tạo đề khớp nhiều lớp/môn, agent hỏi chọn đúng mục. Thông tin quan trọng còn thiếu (chủ đề/học liệu, độ khó, điểm, Practice/Graded, cá nhân/nhóm) được hỏi gộp trước khi ghi; không hỏi lại điều đã biết. “Chấm hết” cần rõ bài tập và có bao gồm các bài đang chấm nháp không; không ghi đè điểm đã chốt/công bố.

Tạo/sửa đề: chat agent → đọc học liệu/schema → lưu DRAFT/đề xuất → trả reviewUrl → mở UI sửa/duyệt. Chấm bài: chọn phạm vi → đọc đủ context → lưu đề xuất → trả reviewUrl từng kết quả → mở UI kiểm tra điểm/bằng chứng. Không tạo lại nội dung để mở duyệt; liên kết grading có proposalId để mở đúng bản kể cả đề xuất cũ. Mở link không áp dụng điểm.

## Backend kiểm tra đọc đủ context

Tool ghi đơn và mỗi item batch bắt buộc gửi snapshotFingerprint lấy từ context. Backend lưu tiến độ đọc liên tiếp theo tài khoản/evaluation/fingerprint trong Redis, hết hạn sau 30 phút không đọc. Bắt đầu offset 0 và theo continuation đến complete=true; không thể nhảy thẳng trang cuối. Bài trống vẫn phải đọc context lần đầu. Progress không chứa bài nộp/token.

MCP_CONTEXT_READ_REQUIRED: đọc lại từ đầu; snapshot không đổi thì giữ requestRef/payload của lần ghi timeout. Snapshot đổi thì đọc/chấm lại và dùng requestRef mới cho nội dung thay đổi. MCP_CONTEXT_READ_UNAVAILABLE: dừng và báo dependency lỗi. Batch item MCP_BATCH_RESULT_LIMIT không ghi nhận tiến độ vì kết quả chưa được giao: đọc riêng trước khi ghi. Client cũ cần tải lại schema tool.

Tiến độ chứng minh server đã phục vụ đủ trang cho tài khoản, không chứng minh model chú ý hoặc chấm đúng. Skill và hook không thay thế giáo viên duyệt.

Hai skill dùng API/tool MCP hiện hành, không chứa token và không thay thế kiểm tra backend:

- [author-assignment](../../agent-skills/author-assignment/SKILL.md): soạn đề và rubric từ học liệu, giữ thang điểm yêu cầu, lưu DRAFT và mở đề xuất để duyệt.
- [grade-with-rubric](../../agent-skills/grade-with-rubric/SKILL.md): đọc đủ snapshot, chấm từng item có bằng chứng, lưu đề xuất để giáo viên duyệt.

Ví dụ: “Dùng author-assignment tạo đề thiết kế phần mềm 12 điểm cho lớp … từ học liệu …”. Hoặc “Dùng grade-with-rubric đề xuất chấm các bài PENDING của bài tập …; báo những mục cần tôi xem lại”.

## Cài đặt có chủ đích

Các file hiện chỉ nằm trong repository; chưa cài vào cấu hình cá nhân. Với client hỗ trợ Agent Skills, sao chép **cả thư mục skill** vào thư mục skill của đúng client rồi tải lại và kiểm tra danh sách skill. Ví dụ Codex dùng `~/.agents/skills/author-assignment/SKILL.md` và `~/.agents/skills/grade-with-rubric/SKILL.md` theo [tài liệu chính thức](https://developers.openai.com/codex/skills/). Các client khác cần kiểm tra đường dẫn theo phiên bản đang cài; đừng coi một đường dẫn chung là tương thích mọi ứng dụng.

Nếu client không tải SKILL.md, mở file và dùng nội dung như hướng dẫn workflow. Hai skill không yêu cầu hook để hoạt động. Token MCP phải có quyền ghi và role phù hợp; việc cài skill không cấp thêm quyền.

## Hook Gemini CLI tùy chọn

Adapter dùng Node đã có trong dự án, không cần Python hay dependency mới. Đối chiếu [BeforeTool/AfterTool chính thức](https://geminicli.com/docs/hooks/reference/) ngày 01/11/2026; chưa nghiệm thu hook bên trong một phiên Gemini có tài khoản thật. Không dùng nguyên định dạng này cho Codex, OpenCode hoặc Antigravity.

- `scripts/mcp-hooks/validate-proposal.mjs`: BeforeTool kiểm tra hình dạng tham số, UTF-8 bytes, requestRef, tổng điểm và độ phủ phản hồi theo điểm đã gửi. Không biết rubric thật nếu agent không gửi đủ, không chứng minh agent đã đọc các trang hay bằng chứng là đúng. Backend vẫn kiểm tra rubric, quyền, hash/version và giới hạn điểm.
- `scripts/mcp-hooks/summarize-result.mjs`: AfterTool chỉ bổ sung số thành công/lỗi, không che kết quả gốc, không tự retry, không in payload hoặc token.

Ghép cấu hình dưới vào `hooks` của Gemini CLI sau khi kiểm tra phiên bản hỗ trợ. Thay đường dẫn bằng **đường dẫn tuyệt đối tới checkout trên máy bạn**, dùng dấu `/` trong JSON. Giữ nguyên hook khác. Tên server phải là `nen-tang-hoc-tap`; nếu đăng ký tên khác, phải sửa matcher và bộ lọc trong adapter trước khi dùng.

```json
{
  "hooks": {
    "BeforeTool": [{
      "matcher": "^mcp_nen[-_]tang[-_]hoc[-_]tap_+record_grading_proposal(s_batch)?$",
      "hooks": [{ "name": "platform-proposal-check", "type": "command", "command": "node \"F:/code/git/AI-Powered-Learning-Platform/scripts/mcp-hooks/validate-proposal.mjs\"", "timeout": 5000 }]
    }],
    "AfterTool": [{
      "matcher": "^mcp_nen[-_]tang[-_]hoc[-_]tap_+record_grading_proposal(s_batch)?$",
      "hooks": [{ "name": "platform-proposal-result", "type": "command", "command": "node \"F:/code/git/AI-Powered-Learning-Platform/scripts/mcp-hooks/summarize-result.mjs\"", "timeout": 5000 }]
    }]
  }
}
```

Kiểm tra adapter bằng `node scripts/mcp-hooks/test_hooks.mjs` từ root. Dữ liệu kiểm tra là giả. Test adapter không chứng minh client thực tế đã nạp hoặc chạy hook. Để tắt, bỏ hai entry có tên `platform-proposal-check`/`platform-proposal-result`, giữ các hook khác.

## Retry và kết quả

Tối đa hai lần retry sau lần đầu cho lỗi tạm thời. Timeout ghi là kết quả chưa rõ: gửi lại **cùng requestRef và cùng payload**, không tạo reference mới cho cả lớp. Đổi nội dung hoặc phải chấm lại snapshot mới thì dùng requestRef mới. `save_assignment_draft` cũ không có requestRef: cần đối chiếu lịch sử đề xuất trước khi gửi lại sau timeout.

Batch tối đa 20 item nhưng toàn bộ arguments chỉ tối đa 64 KiB. Chia nhỏ theo byte UTF-8; phản hồi theo rubric dài thường cần từng bài hoặc vài bài. `partial=true` và HTTP 200 không có nghĩa mọi item thành công. Giữ item thành công, xử lý lỗi riêng.

READY nghĩa là đề xuất đã được nhận qua kiểm tra cấu trúc. [Đánh giá chất lượng chấm](mcp-grading-quality.md) vẫn cần điểm chuẩn của giáo viên.
