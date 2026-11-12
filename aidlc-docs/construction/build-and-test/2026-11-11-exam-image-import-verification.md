# Nhập đề từ ảnh/PDF — build và test, 2026-11-11

Nhánh `feat/exam-image-import` (worktree `.claude/worktrees/exam-image-import`). Xem [tóm tắt mã](../u07-assignment-authoring/code/2026-11-11-exam-image-import-summary.md) và [kế hoạch](../plans/code-generation-plan/2026-11-11-u07-exam-image-import-code-generation-plan.md).

## Lệnh

Backend chạy Maven trong Docker, mount cả worktree để test đọc được `../infra/postgres/init/01-roles.sh`, Testcontainers đi qua Docker socket của máy:

```bash
docker run --rm -v "<worktree>:/ws" -v "$HOME/.m2:/root/.m2" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e TESTCONTAINERS_RYUK_DISABLED=true \
  -w /ws/backend maven:3.9-eclipse-temurin-17 mvn -o -B verify
```

Frontend chạy trong `frontend/` của worktree sau `npm ci`: `npm test`, `npm run lint`, `npm run typecheck`, `npm run build`.

## Kết quả

| Phần | Kết quả |
|---|---|
| Backend `mvn verify` | 2197 test, 0 failure, 0 error, BUILD SUCCESS |
| Frontend Vitest | 968/968 (135 file) |
| Frontend tsc, eslint | sạch |
| Frontend `npm run build` | thành công |

## Lỗi ở lần chạy đầu và cách xử lý

Lần đầu `mvn verify` có 13 lỗi, do hai nguyên nhân:

1. **Lỗi mã: `AiProperties.Provider` không được bind (3 lỗi trong `PlatformWorkflowIntegrationTest`).** Record có thêm constructor 2 tham số bên cạnh constructor chuẩn. Khi đó Spring Boot không tìm được constructor để bind, nên `models` rỗng và mọi model bị từ chối ("Mô hình không nằm trong danh sách cho phép"). Unit test tự tạo đối tượng nên không phát hiện. Đã bỏ constructor phụ; test gọi constructor 3 tham số với `List.of()`.
2. **Lỗi môi trường (10 lỗi trong `JdbcQuizRepositoryIntegrationTest`, `JdbcQuestionRepositoryIntegrationTest`, `JdbcQuizAttemptRepositoryIntegrationTest`).** Lần đó Docker chỉ mount `backend/`, nên không có `../infra/postgres/init/01-roles.sh`. Mount cả worktree thì các test này qua.

## Thử trên trình duyệt với AI thật

Stack Docker riêng (project `examimport`, cổng 28088) chạy mã của nhánh, seed một giảng viên và một lớp. Kết nối AI là Gemini `gemini-3.1-flash-lite`, chép từ stack local chính. Đề mẫu tự tạo gồm: một ảnh JPEG chụp nghiêng 2° có hình đồ thị, một ảnh PNG trang 2, và một PDF scan 2 trang không có lớp chữ.

| Kịch bản | Kết quả |
|---|---|
| Popup tạo bài, Đề mới, Mẫu đề là 2 ảnh | Tạo bài, sang trang soạn bài, popup nhập đề tự mở và gửi AI |
| Chọn chung ảnh và PDF | Báo "Không chọn chung ảnh với PDF hay DOCX trong một lần nhập.", không gọi máy chủ |
| 1 ảnh JPEG, khung đề trống | Chép đúng nguyên văn; 2 tiêu đề, 1 hình; nút "Ghi vào đề" |
| Ghi vào đề | Toàn bộ chữ vào trình soạn, hình đồ thị lưu thành khối ảnh và hiện đúng |
| PDF 2 trang, khung đề đã có nội dung | Đọc đủ Phần I và Phần II; hiện "Chèn vào cuối" và "Thay toàn bộ" |
| Thay toàn bộ, có tích "Dùng … làm tên bài" | Khung đề được thay, tên bài đổi theo đề |
| Nút Hoàn tác | Khôi phục cả khung đề lẫn tên bài |

### Lỗi tìm thấy và đã sửa

1. **Chọn tệp trong popup nhập đề không thêm được tệp nào (frontend).** Trong Chrome, `FileList` của input là đối tượng sống, nên `event.target.value = ""` làm nó rỗng. `addFiles` lại đọc danh sách trong updater của `setFiles`, mà updater chạy sau khi value đã bị xóa. Đã sửa bằng cách chép ra mảng ngay khi nhận sự kiện. Helper `choose` của `ExamImportDialog.test.tsx` giờ mô phỏng `FileList` sống; không có bản sửa thì test đỏ.
2. **Gemini trả khối không có chữ (backend).** Schema chỉ bắt buộc `type`. Gemini bỏ mọi mảng và object con không bắt buộc, nên mọi khối thiếu `runs` và khối FIGURE thiếu `page`/`box`. Kết quả là đề xuất chỉ còn "[Hình: Đồ thị]". Đã sửa: khối bắt buộc có `runs`, `items`, `rows`; vị trí hình chuyển sang mảng `figures` cấp trên cùng, mỗi phần tử bắt buộc có `page`, `box`, `caption`, và khối FIGURE thứ n ứng với `figures[n]`. Mapper vẫn đọc được `page`/`box` nằm trong khối như trước. Đã thêm test `schemaRequiresTheFieldsThatCarryContent`.

3. **Màn xem trước không hiện hình đã cắt (FR-IMP-05.1).** Khối ảnh chỉ có `fileRef`, mà `DocumentViewer` chỉ hiện ảnh đã lưu hoặc ảnh có `previewUrl`. Đã sửa: thêm `IArtifact.issuePreviewToken` (BR-U03-101). `GET` bản nháp nhập đề READY thêm `proposal.previews` (id khối → link tải 5 phút), cấp mỗi lần đọc và không lưu. `DocumentViewer` nhận thêm `previews`. Đã thêm test ở `ArtifactServiceTest`, `AssignmentImportServiceTest` và `ExamImportDialog.test.tsx`. Thử lại trên trình duyệt: hình hiện trong màn xem trước.
4. **"Chèn vào cuối" thay toàn bộ khung đề (backend).** Request `AssignmentRequests.Apply` không có trường `placement`, nên mọi lần ghi đều là REPLACE. Unit test gọi thẳng service nên không bắt được. Đã thêm trường, và thêm `AssignmentRequestsTest` đọc body JSON. Thử lại: audit ghi `APPEND`, khung đề 11 + 6 = 17 khối, Phần II nối sau Phần I.

Sau các bản sửa: backend `mvn verify` 2201/2201, BUILD SUCCESS; frontend Vitest 969/969 (135 file), tsc, eslint sạch.

### Sau khi merge `origin/staging` (2026-11-11)

Merge 28 commit của staging (PR #70–#75, gồm #72 gắn ciphertext key AI với kết nối, và tính năng sinh viên tự chia nhóm). Có một xung đột mã ở `AiTaskJobHandler.dispatch`: giữ tham số ảnh của nhánh này và dùng `cipher.decryptConnectionKey(connection)` của staging. `audit.md` và `aidlc-state.md` giữ mục của cả hai nhánh. Sau merge: backend `mvn verify` 2264/2264, BUILD SUCCESS; frontend Vitest 998/998 (141 file), tsc, eslint sạch. Không thử lại trên trình duyệt sau merge.

### Merge staging lần 3 (2026-11-12, cho PR #76)

Staging thêm `1a36439b` (không gian làm bài nhóm) và `456356a5` (trợ lý AI sửa đề nổi, xuất DOCX, popup tạo bài mặc định "Đề có sẵn"). Xung đột ở 3 tệp frontend:
- `api/index.ts`: giữ `importDraft`, nhận `listAiTemplates(own)` của staging.
- `CreateAssignmentDialog.tsx`: mặc định `EXISTING` của staging, giữ `templateFiles` nhiều tệp.
- `AssignmentFormPage.tsx`: giữ mục "Nhập đề từ tệp" và `ExamImportDialog`, nhận `downloadDocx`, nhãn "Tải đề (DOCX)" và `AiReviseChat` nổi.

Test "ảnh đề cũ" của popup tạo bài thêm bước chọn "Đề mới", vì mặc định giờ là "Đề có sẵn". Sau merge: backend `mvn verify` 2273/2273, BUILD SUCCESS; frontend Vitest 1018/1018 (146 file), tsc, eslint sạch. Chưa thử lại trên trình duyệt.

### Còn tồn tại

- Khung cắt hình do AI ước lượng hơi rộng: với ảnh chụp nghiêng, hình dính thêm một dòng chữ phía trên.
- Ctrl+Z ngay sau khi popup đóng không hoàn tác được, vì tiêu điểm nằm ở `body` chứ không ở trình soạn; nút Hoàn tác thì chạy. Phím tắt của trình soạn vốn hoạt động như vậy, không riêng phần nhập đề.
- Chưa thử nhập DOCX trên trình duyệt (đã có test Vitest), và chưa thử OpenAI/Claude.
- Chưa đo hiệu năng dựng PDF và chuẩn hóa ảnh với tệp lớn (10MB, 10 trang). Một ảnh mất khoảng 45 giây với `gemini-3.1-flash-lite`.
