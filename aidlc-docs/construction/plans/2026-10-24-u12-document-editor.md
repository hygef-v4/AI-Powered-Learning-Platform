# U12 – dùng trình soạn chung (Draw.io) cho tài liệu nhóm (KAN-64, KAN-66)

Trạng thái: **kế hoạch, chờ người dùng duyệt**. Nhánh `feat/u12-document-editor` từ `origin/staging` (6337b42).

## Vì sao
KAN-64 và KAN-66 yêu cầu dùng Draw.io (tuỳ chọn) trong bài nhóm. Tài liệu nhóm đang dùng trình soạn đoạn văn tạm:
- `SectionDialogs.tsx` dùng textarea và `textToBlocks`;
- `BlockView.tsx` chỉ hiện chữ "[Sơ đồ Draw.io]".

Trình soạn chung `@/shared/document` đã có trên staging (`DocumentEditor`, `DocumentViewer`, `DiagramEditor`).

## Các bước
- [ ] 1. `SectionEditorDialog`: thay textarea bằng `DocumentEditor` ở `mode="student"`.
  - Chỉ soạn nội dung của mục; `diagramEnabled` lấy từ bài.
  - Giữ tự lưu 10 giây, "Bạn đang giữ", "Tự lưu lúc …", nút Xong.
  - Backend đã kiểm nháp trong khung chung (`GroupSnapshot.withSection`, PR #19), nên block STUDENT nào cũng nhận.
- [ ] 2. Hiển thị nội dung mục bằng `DocumentViewer` thay cho `BlockView` (có sơ đồ, ảnh, bảng). Sơ đồ dùng `DiagramViewPage` nếu cần.
- [ ] 3. Backend: thêm vào `WorkspaceView` cờ `diagramEnabled` (lấy từ `AssignmentContent`) và số sơ đồ bắt buộc; ảnh nếu có thì dùng `uploadImage` qua U03.
- [ ] 4. Gắn `useActivityTracker` (`shared/activity`) khi bài bật giám sát hoạt động. Ghi vào `POST /api/v1/group-docs/{id}/activity` (endpoint đã có).
- [ ] 5. Flyway: thêm khóa ngoại `group_documents.assignment_id` → `assignments(id)`, vì bảng U07 đã có trên staging. Kiểm `out-of-order`.
- [ ] 6. Test:
  - frontend: SectionEditorDialog dùng DocumentEditor; tự lưu; Xong;
  - backend: `WorkspaceView.diagramEnabled`; migration;
  - chạy lại `scratchpad/e2e/u12b.mjs` trên stack local.
- [ ] 7. Cập nhật code-summary U12, aidlc-state, audit; mở PR vào `staging`.

## Ghi chú để làm tiếp
- Stack local: `docker compose -p platform-local -f infra/docker-compose.yml -f infra/docker-compose.local.yml --env-file infra/.env.local up -d --build`.
  Nginx ở cổng 8088, Mailpit ở cổng 8025.
- Script test end-to-end nằm trong scratchpad của phiên (`e2e/seed.mjs`, `u02.mjs`, `u06.mjs`, `u06b.mjs`, `u12.mjs`, `u12b.mjs`). Mật khẩu tài khoản thử nằm trong `infra/.env.local`.
- `IDocumentModel` trong unit test là mock và đã từng che lỗi. Phải có ít nhất một test dùng `DocumentModelService` thật.
