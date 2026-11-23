# Date, AI and grading-window implementation plan

**Goal:** thực hiện yêu cầu đã duyệt, dùng helper và hợp đồng hiện có với diff nhỏ nhất.
**Spec:** [yêu cầu](../../../inception/requirements/2026-11-15-date-ai-grading-window.md).
**Execution:** ba phần độc lập, test RED trước code; root tích hợp và tự review.

- [x] DateInput chung và test ngày/tháng/năm nhuận/min/max/keyboard/native picker;
  thay toàn bộ caller date/datetime-local, kiểm test feature và typecheck.
- [x] Probe AI và nhãn lỗi: test token-limit/200, budget thực tế, sai key/quota/rate
  limit; giữ schema/truncation validation của tác vụ sinh nội dung; không gọi key thật.
- [x] Backend close: kiểm quyền/version, giữ hạn gốc, chốt individual/group, audit;
  tự động đóng sau hạn cuối +30 giây qua cùng cơ chế an toàn.
- [x] Guard chấm/công bố ở backend dùng chung: manual/AI/member/batch/MCP, trạng thái
  read-only của detail; test trước hạn, trong late/grace, sau đóng và đóng sớm.
- [x] UI đóng nhận bài/xác nhận, thông báo chỉ đọc trong workspace và batch; test caller.
- [x] Tích hợp, chạy kiểm phù hợp, rà phân quyền/concurrency/không mất bài và cập nhật
  business rules, state, kết quả, audit. Không commit/push/merge/deploy.

Kết quả: [kiểm chứng và giới hạn](../../build-and-test/2026-11-15/2026-11-15-date-ai-grading-window.md).
