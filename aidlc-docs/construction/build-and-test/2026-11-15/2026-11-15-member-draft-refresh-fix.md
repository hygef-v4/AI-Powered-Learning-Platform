# U09 — giữ bản nháp khi refresh điểm nhóm lỗi

Nhánh `fix/grading-member-drafts`, sửa P2 từ review PR #87 theo
[kế hoạch](../../plans/code-generation-plan/2026-11-15-member-draft-refresh-fix.md).

## Thay đổi

`useGradingWorkspace` giữ nguyên thành viên và đặt `editable: false` khi đã lưu điểm
nhóm nhưng refresh thất bại. Form vẫn mount nên điểm/mức/căn cứ chưa lưu và cảnh báo
rời trang còn nguyên. Caller hiện có khóa ghi/công bố bằng `editable`; reload thành
công nhận lại quyền/version từ server, giữ quy tắc rebase/conflict và reset bài nộp.
Không thêm state, API hoặc nơi lưu draft mới.

## Kiểm chứng ngày 2026-11-15

- RED: test mới trong `workspace.test.tsx` thất bại đúng nguyên nhân: sau save thành
  công/refresh lỗi không tìm thấy ô Căn cứ đánh giá vì các form bị unmount.
- GREEN: cùng test qua sau một dòng sửa hook. Kiểm giữ cả điểm, mức và căn cứ;
  khóa lưu/công bố và giữ cảnh báo rời trang; reload lỗi tiếp vẫn giữ draft; reload
  thành công cho lưu bằng version mới (3).
- `npx vitest run tests/features/grading tests/shared/document tests/features/assignmentwork --no-file-parallelism`:
  **306/306 test, 33/33 file qua** (64,36 giây). Có cảnh báo jsdom `Window.scrollTo`
  từ test hiện hữu; không có test lỗi.
- ESLint hai file sửa: qua.
- `npm run typecheck`: lỗi cache `.next/types` còn trỏ tới ba route đã xóa
  (`admin/mcp`, `ai/usage`, đường dẫn submissions lồng trong lớp). Đây là giới hạn
  cục bộ đã ghi trong báo cáo grading trước đó.
- Typecheck toàn bộ `src/tests` và `next-env.d.ts` bằng cấu hình tạm kế thừa
  `tsconfig.json`, `incremental: false`, loại `.next`: qua. Đã xóa đúng file cấu
  hình tạm; không sửa cache, tsconfig hoặc cấu hình Next sản phẩm.
- Diff nguồn/test không có lỗi whitespace. Tự review caller: cùng hook cho lưu tay
  và autosave; `editable: false` khóa form nhóm, form thành viên và công bố; reload
  thành công khôi phục theo response; không gỡ form khi lỗi tạm thời.

## Phạm vi và giới hạn

Security: quyền/version backend giữ nguyên, chặn ghi bằng dữ liệu thành viên cũ.
Resiliency: draft không mất qua lỗi refresh lặp lại. PBT tắt theo cấu hình repo.
Không đổi backend/schema/migration, không gọi AI hoặc ghi điểm thật.
Chưa chạy lại full frontend/backend, build hay e2e/trình duyệt live. Chưa commit,
push, cập nhật PR hoặc deploy. Hai nhận xét P3 và rủi ro rollback migration ngoài
phạm vi bản sửa này.

## Kiểm tra trước commit

Theo yêu cầu commit tiếp theo, chạy lại workspace, preserve-grading và
MemberContributionPanel: 65/65 test, 3/3 file qua; `git diff --check` sạch.
Commit cục bộ trên `fix/grading-member-drafts`; chưa push hoặc cập nhật PR #87.
