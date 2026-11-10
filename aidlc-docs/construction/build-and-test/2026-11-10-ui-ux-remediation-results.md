# Sửa 22 điểm UI/UX — build và kiểm thử, 2026-11-10

Nguồn: [yêu cầu và quyết định](../../inception/requirements/2026-11-10-ui-ux-remediation-questions.md), [kế hoạch và điều chỉnh](../plans/code-generation-plan/2026-11-10-ui-ux-remediation.md), [tóm tắt thay đổi](../ui-ux-remediation/code/summary.md). Nhánh `fix/ui-ux-remediation`, cây làm việc chưa commit trên nền `staging` `062a43f5`.

Thay đổi chỉ nằm trong `frontend/` và `PRODUCT.md`. Hướng dẫn build và kiểm thử chung của dự án không đổi: [build](build-instructions.md), [unit test](unit-test-instructions.md), [integration test](integration-test-instructions.md), [performance test](performance-test-instructions.md), [E2E](e2e-test-instructions.md). Tài liệu này chỉ ghi kết quả của đợt sửa.

## Lệnh đã chạy

Chạy trong `frontend/`, Node 20 trở lên:

```bash
npm install
npx tsc --noEmit
npx eslint .
npx vitest run
npm run build
```

## Kết quả

| Phạm vi | Kết quả |
|---|---|
| TypeScript (`tsc --noEmit`), sau production build | Không lỗi |
| ESLint toàn bộ `frontend/` | Không lỗi, không cảnh báo |
| Vitest toàn bộ | 886 ca / 128 file qua, ba lần chạy liên tiếp |
| Production build (`npm run build`, webpack) | Thành công; mọi route biên dịch, gồm 52 trang vừa thêm `metadata.title` |
| `git diff --check` | Sạch |
| Trình duyệt, dev server với backend Docker local | Xem bảng dưới |

Số ca kiểm thử tăng từ 867 lên 886. 19 ca mới thuộc: Toast (4), theme (3), Dialog (1), Segmented, Menu, bảng xếp thẻ và khung xương (4), AppShell (2), form đăng nhập (1), màn làm quiz (2), popup xác nhận đề xuất AI (1), trang lỗi (1). Sáu ca sẵn có được sửa theo hành vi mới: vị trí Đăng xuất, nhãn sidebar thu gọn, vai trò `menuitem` của link tài khoản, popup xác nhận thay `window.confirm`, hàm báo lỗi của popup soạn AI.

### Kiểm tra trên trình duyệt

| Màn | Điều đã thấy |
|---|---|
| Đăng nhập, 1024px, theme sáng | Tiêu đề tab "Đăng nhập · Nền tảng học tập". Gửi form trống: "Nhập email trường hợp lệ." dưới ô Email, "Nhập mật khẩu." dưới ô Mật khẩu, cả hai ô có `aria-invalid`, focus ở ô Email. Nút hiện mật khẩu 32×32px, hai link cao 24px |
| Không tìm thấy trang, 375px, hệ điều hành đặt tối | Tiêu đề tab "Không tìm thấy trang · Nền tảng học tập". Chưa lưu lựa chọn theme thì trang dùng theme tối. Tiêu đề nét 700, có nút đổi theme, không tràn ngang (`scrollWidth` 375) |

## Chưa kiểm tra

- **Mọi màn sau đăng nhập trên trình duyệt**: khung trang và ngăn kéo mobile, menu tài khoản, nhãn nổi của sidebar thu gọn, màn làm quiz (thanh dính, mốc thời gian, Thử lại), ba bảng xếp thẻ ở 375px, bộ lọc `Segmented`, popup xác nhận lồng trong popup, toast. Phiên làm việc không đăng nhập được tài khoản thử; các phần này chỉ có kiểm thử tự động trong jsdom, không kiểm tra được bố cục, màu và vùng chạm thật.
- Theme tối của các màn sau đăng nhập và độ tương phản của token màu mới (`viewer`, `sheet-*`, `code-*`; giá trị giữ nguyên như trước).
- Trình đọc màn hình thật (NVDA, VoiceOver, TalkBack) và thiết bị cảm ứng thật cho biến thể `pointer-coarse`.
- Backend, E2E và performance test: không chạy vì không có thay đổi backend, API hay luồng dữ liệu. Lần chạy đầy đủ gần nhất là trên `63e91aa0` (backend 2.081 ca).

## Ghi chú

- Một ca của `SubjectDetailMutations.test.tsx` trượt chập chờn trong lúc làm: bản đầu của Toast cho mọi câu màu đỏ dùng `role="alert"` và không tự ẩn, nên câu "Đã lưu trữ môn học" xuất hiện trước khung báo lỗi đọc. Đã sửa bằng cách tách `showErrorToast` cho lỗi thật; sau đó bộ kiểm thử qua ba lần liên tiếp.
- `node_modules` ban đầu thiếu `xlsx`, `docx-preview`, `pptx-preview`, `pdfjs-dist`, làm 7 file kiểm thử không nạp được. Đã chạy `npm install`; `package-lock.json` chỉ bị đổi thứ tự và đã hoàn nguyên.
- Console của dev server có lỗi CSP về inline style. Lỗi này có từ trước thay đổi; các style mới thêm đều đặt qua CSSOM sau khi trang chạy, không nằm trong HTML do máy chủ trả về.

## Extension

- Security Baseline (phạm vi đã duyệt): tuân thủ. Không đổi xác thực, phân quyền, CSP hay dữ liệu gửi đi; script chọn theme vẫn chạy qua nonce. Các quy tắc còn lại N/A vì không có thay đổi backend hay dữ liệu.
- Resiliency Baseline (phạm vi đã duyệt): tuân thủ. Nút Thử lại ở màn làm quiz gọi lại hàng đợi lưu sẵn có, không thêm cơ chế gọi lại mới. Các quy tắc còn lại N/A.
- Property-Based Testing: tắt theo cấu hình dự án.
