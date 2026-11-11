# Kết quả ba lỗi UI/UX — 2026-11-11

Baseline origin/staging ce40e447, worktree `.claude/worktrees/ui-ux-staging`,
nhánh `codex/ui-ux-staging`. Người dùng duyệt: "sửa 3 lỗi trên".

## Thay đổi

- `frontend/src/shared/ui/Pagination.tsx` và `PageNav.tsx`: giới hạn nav theo vùng chứa,
  cho phép xuống dòng, giữ thứ tự, toàn bộ nút và kích thước chạm 44px. Trước đó
  Pagination có 9 phần tử cần tối thiểu 444px khi pointer coarse.
- `frontend/src/shared/ui/inputs.tsx`: Switch nối nhãn hiển thị bằng aria-labelledby
  khi không có ariaLabel; nhãn tường minh vẫn ưu tiên.
- Field cấp id cho lỗi và nối aria-describedby tới control có id khớp htmlFor,
  đi qua wrapper DOM/Fragment, giữ mô tả hiện có; hỗ trợ TextInput/Select và native
  input/select/textarea. Không thay validation hay gán lỗi cho control bên cạnh.
  Giữ identity/focus của ô nhập khi lỗi xuất hiện/biến mất. Component tùy biến
  cần forward aria-describedby nếu dùng làm control trực tiếp của Field.

## Kiểm thử và lệnh tái hiện

Từ `frontend/`:

```text
npm test -- tests/shared tests/features/identity tests/features/subjects tests/features/settings tests/features/assignments
npm run typecheck
npm run lint
npm run build
```

- 5 ca hồi quy mới thất bại đúng kỳ vọng trên baseline, 2 ca Switch cũ qua.
- Lượt cuối: **332 test / 51 file qua**, gồm shared UI và các caller nêu trên.
- Test Field xác nhận mô tả cũ cộng lỗi, xóa liên kết khi hết lỗi, giữ focus/DOM node,
  không nhầm control, shared select và native input/textarea có lỗi riêng.
- Test Switch xác nhận nhãn JSX dự phòng, ưu tiên ariaLabel, disabled và hành vi
  pending/success/failure cũ.
- TypeScript, full ESLint, scoped ESLint cuối và git diff --check: qua.
- Build production kết thúc exit 0, compile và prerender 32/32 thành công.

## Giới hạn

- Không xác minh bố cục bằng trình duyệt hoặc thiết bị cảm ứng thực. Lượt trước
  Computer Use dừng vì không xác nhận URL Chrome. jsdom không mô phỏng layout CSS;
  sửa responsive được kiểm tra bằng source, utility CSS và build.
- Dùng junction node_modules tới dependency hiện có ở checkout gốc để không cài lại.
  Vì vậy build cảnh báo 27 traced files ở ngoài outputFileTracingRoot không được
  đưa vào standalone output. Output này chưa được xác minh để triển khai độc lập;
  cần build với dependency nằm trong checkout trước khi deploy.
- Build cũng cảnh báo fetch font động của ảnh Open Graph bị EACCES trong sandbox.
  Không sửa cấu hình production để che các cảnh báo môi trường này.
- Không chạy toàn bộ frontend/backend suite, không sửa API/schema/backend,
  không thao tác dữ liệu thật, commit, push hoặc deploy.

## Extension

Security và Resiliency giữ phạm vi đã duyệt: không thay quyền/log/xác thực,
không thêm nội dung lỗi hoặc dữ liệu nhạy cảm, giữ hành vi lưu bất đồng bộ;
kiểm thử liên quan qua. Các quy tắc hạ tầng, DR, deployment N/A cho delta UI.
Property-Based Testing bỏ qua theo cấu hình disabled.

## Kiểm tra sẵn sàng vào staging — yêu cầu tiếp theo, 2026-11-11

Người dùng: "kiểm tra xem có thể đưa lên staging chưa".

- Fetch origin/staging: vẫn `ce40e4472e68fb7dac8a7884d9e22b8fcb3a8c3e`, khớp HEAD
  của worktree. Không có upstream delta hay xung đột cần xử lý tại thời điểm kiểm tra.
- Review ba file production, hai file test và CI: không phát hiện lỗi chặn tích hợp.
  Package/lockfile/config/Dockerfile/backend không thay đổi; các kiểm tra TypeScript
  và ESLint đã qua trên cùng source ở lượt trước vẫn có hiệu lực.
- `npm test`: **963/963 test, 135/135 file qua**. Cảnh báo scrollTo/navigation là giới hạn jsdom.
- `docker build --tag platform-ui-ux-staging-check:20261111 frontend`: thành công bằng
  Dockerfile hiện có. Layer dependency npm ci được cache; compile, TypeScript,
  prerender và copy standalone thành công. Không có cảnh báo font/tracing của lượt
  Windows dùng junction. Image: `sha256:c114fcb3696c79934d102aeae5f1a08702e8627a55962fde039bdfb7a166efcd`.
- Smoke test image bằng `docker run --rm --network none`, không publish port,
  server chỉ bind loopback trong container: `/api/health` trả 200 và status UP;
  `/login`, `/icon.svg` trả 200. Container tự xóa sau kiểm tra; image giữ lại local.
- Diff check qua. Source không thay đổi trong lượt review này.

**Kết luận:** đủ điều kiện commit và tạo PR vào staging; nên merge sau khi CI PR
qua. CI của repo chạy khi pull_request, còn push trực tiếp chỉ trigger với main.
Chưa commit/push/tạo PR/merge trong lượt kiểm tra này, vì yêu cầu là đánh giá readiness.
Chưa có CI GitHub trên các thay đổi chưa commit. Không chạy lại backend vì không
thay đổi backend/API/schema; CI PR vẫn chạy backend verify theo cấu hình.

Giới hạn còn lại: chưa kiểm tra layout trực tiếp trên browser/mobile. Cảnh báo
đóng gói standalone ở lượt Windows trước đã được giải quyết về mặt xác minh bằng
build và smoke test Docker thành công; không dùng artifact Windows cũ để deploy.

## Tích hợp staging trước khi mở PR — 2026-11-11

Staging tiến tới `1f713615` (teacher class overview) trong lúc chuẩn bị PR.
Source merge tự động, chỉ audit/state xung đột append; giữ toàn bộ nội dung cả hai
bên, xác minh từng dòng theo thứ tự. Fresh full suite **963 test/135 file**, full
ESLint và TypeScript qua trên bản tích hợp. Docker build thành công, image
`sha256:289cbf8a2995fda2d63109b4c83ed6e679b2482a921b92802b4e80dab81b4d52`, smoke
cô lập health UP/login/icon đều 200. Source diff so với staging vẫn chỉ ba file
shared UI và hai file test. Không chạy lại backend/live browser. User đã cho phép
commit/push/tạo PR; chưa cho phép merge PR. PR GitHub ghi nhận trạng thái CI/phát hành.
