# U12 — Nhập nhóm từ Excel: kế hoạch code generation

Yêu cầu và quyết định: `aidlc-docs/inception/plans/2026-11-27-group-import-questions.md`, BR-U12-99…103. Chỉ frontend; không đổi backend, API, schema.

- [x] `frontend/src/features/groups/model/groupWorkbook.ts`: `downloadGroupTemplate` (sinh mẫu theo lớp bằng SheetJS), `readGroupWorkbook` (đọc, kiểm file), `buildGroupImport` (ghép dòng thành nhóm mới, lỗi từng dòng, cảnh báo sĩ số và đề xuất).
- [x] `frontend/src/features/groups/components/GroupImportDialog.tsx`: tải mẫu, chọn file, bản xem trước, chọn trưởng nhóm còn thiếu, lưu qua `saveGroups`.
- [x] `ProposalReviewList`: nút "Nhập từ Excel" cạnh Cài đặt; `ClassGroupsPanel` truyền bộ nhóm, danh sách sinh viên và hàm nhận kết quả lưu.
- [x] Khóa i18n vi, en.
- [x] Test: `tests/features/groups/model/groupWorkbook.test.ts`, test dialog; chạy test groups, typecheck, lint.
- [x] Kiểm trên trình duyệt local (lớp SE-MAU1, giao diện tiếng Anh): nút cạnh Settings, file có 1 dòng ngoài lớp báo bỏ qua, cảnh báo sĩ số, lưu tạo nhóm, xóa nhóm thử sau khi kiểm.
