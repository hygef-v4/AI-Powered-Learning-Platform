# U12 — Nhập nhóm từ Excel: kết quả build và test (2026-11-27)

Phạm vi: BR-U12-99…103, [kế hoạch](../../plans/code-generation-plan/2026-11-27-u12-group-excel-import.md). Chỉ frontend; không đổi backend, API, migration.

## Build

| Lệnh (trong `frontend/`) | Kết quả |
|---|---|
| `npm run typecheck` | Sạch |
| `npm run lint` | 0 lỗi; 2 cảnh báo có sẵn ở `assignmentwork/pages/AssignmentWorkspacePage.tsx`, ngoài phạm vi |
| `npm run build` | Đạt, 31 trang tĩnh |

## Unit test

| Lệnh | Kết quả |
|---|---|
| `npx vitest run` (toàn bộ) | 1273/1273 đạt |
| `tests/features/groups/model/groupWorkbook.test.ts` | Đọc cột theo tiêu đề vi/en, từ chối thiếu cột, công thức, quá 500 dòng, không phải xlsx, quá 200 KB; ghép nhóm, lỗi từng dòng, nhiều trưởng nhóm, cảnh báo sĩ số và đề xuất |
| `tests/features/groups/model/groupTemplate.test.ts` | Mẫu có hai sheet, điền sẵn sinh viên, đọc lại được |
| `tests/features/groups/components/GroupImportDialog.test.tsx` | Xem trước, chặn lưu khi thiếu trưởng nhóm, gửi cả bộ nhóm kèm `version`; báo lỗi file thiếu cột |

## Integration (trình duyệt local)

Backend `platform-backend:local` dựng lại từ mã hiện tại; frontend `next dev`; tài khoản giảng viên local, lớp SE-MAU1, giao diện tiếng Anh.

- Nút Import from Excel cạnh Settings khi đợt tự chia nhóm đang tắt.
- File 2 dòng (1 sinh viên của lớp theo email, 1 MSSV ngoài lớp): bản xem trước báo bỏ qua dòng 3, cảnh báo "Fewer than 2", trưởng nhóm theo dấu x.
- Lưu: `PUT /classes/{id}/groups` thành công, nhóm hiện trong thẻ Nhóm, sinh viên có nhãn Group leader. Nhóm thử đã xóa sau khi kiểm.

## Chưa kiểm

- Bấm Tải file mẫu trên trình duyệt (nội dung mẫu kiểm bằng test).
- Test hiệu năng: không áp dụng (đọc tối đa 500 dòng ở trình duyệt). Không có test backend mới vì không đổi backend.
- Chưa chạy trên staging.
