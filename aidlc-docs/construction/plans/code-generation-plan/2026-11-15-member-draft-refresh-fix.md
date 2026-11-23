# U09 — giữ bản nháp thành viên khi refresh lỗi

Theo yêu cầu sửa lỗi P2 đã xác nhận trong review PR #87. Đây là phần sửa tiếp của thiết kế
[chấm cá nhân cạnh bài nhóm](2026-11-13-group-grading-side-panel.md), không đổi yêu cầu nghiệp vụ.

Phạm vi: nhánh xử lý lỗi trong `useGradingWorkspace`, caller `GradingWorkspacePage`,
test workspace/hook/panel hiện có và tài liệu kiểm chứng. Không đổi backend, API, schema,
công thức điểm, hai mục đặt tên P3 hoặc cơ chế deploy.

Giữ danh sách thành viên khi lưu nhóm thành công nhưng refresh lỗi. Dùng `editable: false`
hiện có để khóa ghi và công bố tới khi reload thành công; giữ form cùng cảnh báo rời trang.
Reload lấy lại version từ server, giữ quy tắc rebase/conflict và reset khi đổi bài nộp hiện có.

- [x] Test hồi quy: giữ điểm/mức/căn cứ sau refresh lỗi, khóa ghi, giữ cảnh báo rời trang,
  reload lỗi tiếp không mất draft, reload thành công mở ghi theo version mới.
- [x] Sửa tối thiểu trong hook, giữ xử lý lỗi hiện có.
- [x] Chạy test grading cùng caller liên quan, TypeScript và ESLint.
- [x] Tự review diff, ghi kết quả và state/audit; không commit/push/merge/deploy.

Kết quả: [306 test qua, kiểm kiểu và giới hạn cache Next](../../build-and-test/2026-11-15/2026-11-15-member-draft-refresh-fix.md).

Security: giữ phân quyền/version phía server và khóa ghi khi dữ liệu chưa cập nhật.
Resiliency: giữ draft qua lỗi tải tạm thời. PBT disabled theo cấu hình repo.
