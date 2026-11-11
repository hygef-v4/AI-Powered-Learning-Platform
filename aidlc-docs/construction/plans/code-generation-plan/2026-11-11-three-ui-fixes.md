# Ba lỗi UI/UX trên staging

Phạm vi người dùng duyệt: "sửa 3 lỗi trên". Baseline origin/staging ce40e447,
nhánh codex/ui-ux-staging. Chỉ shared frontend UI và tài liệu/kiểm thử liên quan.

## Yêu cầu và tiêu chí

1. Pagination và PageNav không buộc một hàng rộng hơn vùng chứa ở màn nhỏ;
   giữ đầy đủ nút, thứ tự bàn phím, trang hiện tại và vùng chạm 44px.
2. Switch dùng nhãn hiển thị làm accessible name nếu caller không truyền ariaLabel;
   giữ ưu tiên nhãn tường minh, trạng thái disabled và lưu bất đồng bộ.
3. Field nối lỗi hiện có với control có id khớp htmlFor bằng aria-describedby;
   giữ mô tả có sẵn, hỗ trợ shared inputs và native input/select/textarea,
   bỏ liên kết lỗi khi lỗi hết, không gán lỗi sang trường khác.

## Quy trình thích ứng

Workspace Detection -> Requirements đã duyệt -> kế hoạch sửa giới hạn -> Code Generation
-> Build and Test. Dùng tài liệu brownfield hiện có. Bỏ qua reverse engineering mới,
stories mới (sửa lỗi hiện hữu), thiết kế ứng dụng/domain/NFR/hạ tầng và chia unit.
Shared UI phục vụ các unit hiện có; không đổi nghiệp vụ, API, DB hay dependency.

## Các bước

- [x] 1. Xác nhận baseline, đọc quy ước và caller/test hiện có.
- [x] 2. Thêm hồi quy cho tên Switch và liên kết lỗi Field; 5 ca mới thất bại đúng lỗi trước sửa, 2 ca cũ qua.
- [x] 3. Sửa ba lỗi trong inputs.tsx, Pagination.tsx, PageNav.tsx.
- [x] 4. Chạy kiểm thử shared/caller liên quan: 332 test/51 file qua; TypeScript, ESLint qua; build exit 0 với cảnh báo font/tracing do môi trường.
- [x] 5. Ghi kết quả và giới hạn xác minh; kiểm tra diff và trạng thái.

Kết quả: [báo cáo](../../build-and-test/2026-11-11-three-ui-fixes.md).

## Extension

Security 03/08/09/12/15: giữ nguyên log, quyền, xác thực và xử lý lỗi; lỗi hiện có
chỉ được nối tới control, không thêm dữ liệu nhạy cảm. Các quy tắc hạ tầng/security
còn lại N/A cho delta UI này. Resiliency 03/04/14: checkout riêng, diff và hồi quy;
các quy tắc runtime/DR còn lại N/A vì không đổi runtime/deploy. PBT disabled theo state.

## Kiểm chứng

Chạy từ frontend: npm test -- tests/shared tests/features/identity tests/features/subjects
tests/features/settings tests/features/assignments; npm run typecheck; npm run lint;
npm run build. DOM/jsdom không chứng minh bố cục thực tế; báo riêng giới hạn browser.

## Kiểm tra sẵn sàng vào staging theo yêu cầu tiếp theo

- [x] Fetch và so sánh HEAD với origin/staging: cùng ce40e447, không có upstream delta.
- [x] Review source diff và CI; không đổi dependency/config/API/backend.
- [x] Toàn bộ frontend suite: 963 test / 135 file qua.
- [x] Build Docker bằng Dockerfile hiện có; standalone đóng gói đủ, không có cảnh báo font/tracing.
- [x] Smoke test image qua HTTP local trong container cô lập: health UP, login/icon 200; đủ điều kiện tạo PR staging, chờ CI PR trước merge.
