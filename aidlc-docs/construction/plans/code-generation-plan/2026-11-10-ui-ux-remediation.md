# Sửa 22 điểm UI/UX

Nguồn yêu cầu và quyết định: [câu hỏi xác nhận 2026-11-10](../../../inception/requirements/2026-11-10-ui-ux-remediation-questions.md), cả 9 câu chọn A. Nhánh `fix/ui-ux-remediation`. Chỉ sửa `frontend/` và `PRODUCT.md`; không đổi backend, API, schema, `data-testid` hay luồng nghiệp vụ. Tuân theo [quy ước mã nguồn](../../code-conventions.md). Không commit (Câu 8 = A).

Kế hoạch này là nguồn duy nhất cho Code Generation: chỉ làm đúng các bước dưới đây, xong bước nào đánh dấu `[x]` bước đó.

## Nhóm A — Component dùng chung (`frontend/src/shared`)

- [x] Bước 1 — `ui/Toast.tsx` (mục 6): toast `danger` không tự ẩn; mọi toast có nút đóng; dừng đếm giờ khi rê chuột hoặc focus; `danger` dùng `role="alert"`, còn lại `role="status"`; bỏ `aria-live` trùng ở khung ngoài. Thêm `tests/shared/ui/Toast.test.tsx`.
- [x] Bước 2 — `ui/Dialog.tsx` (mục 15): khóa cuộn `body` khi có popup mở và trả lại khi popup cuối đóng; tiêu đề cỡ `md` 22px, `lg`/`xl` giữ 26px. Bổ sung ca trong `tests/shared/ui/Dialog.test.tsx`.
- [x] Bước 3 — `ui/Menu.tsx` (mục 14): `aria-haspopup="menu"` và `aria-expanded` trên nút mở; mũi tên lên/xuống, Home, End di chuyển giữa các `[data-menu-item]`; mở menu thì focus mục đầu, Esc trả focus về nút mở. `layout/AppShell.tsx`: `AccountLink` thêm `role="menuitem"`. Thêm ca kiểm thử trong `tests/shared/ui/ui.test.tsx`.
- [x] Bước 4 — `ui/inputs.tsx` (mục 12, phần lõi): `Segmented` dùng roving tabindex; mũi tên trái/phải, Home, End đổi lựa chọn và đưa focus theo, bỏ qua mục `disabled`; nhận thêm `count` cho từng lựa chọn. Thêm ca kiểm thử trong `tests/shared/ui/ui.test.tsx`.
- [x] Bước 5 — Vùng chạm (mục 8): thêm biến thể `pointer-coarse` trong `styles/globals.css` nếu Tailwind chưa có sẵn; `ui/Button.tsx` (`IconButton`), `ui/Pagination.tsx`, `ui/inputs.tsx` (`Switch`, `Segmented`), nút đóng của `Dialog`, nút số câu trong `features/quizpractice/components/QuestionPager.tsx` có vùng chạm 44px trên màn cảm ứng, kích thước nhìn thấy trên máy tính không đổi. Nút hiện mật khẩu (`features/identity`) tối thiểu 32px; hai link của form đăng nhập có vùng bấm cao tối thiểu 24px.
- [x] Bước 6 — `ui/Table.tsx` và component mới `ui/Skeleton.tsx` (mục 16): `Skeleton` là khối xám `aria-hidden`; `TableSkeletonRows` vẽ n dòng khung xương theo số cột, bọc trong dòng có `role="status"` và nhãn đọc màn hình. Xuất qua `ui/index.ts`. Thêm ca kiểm thử trong `tests/shared/ui/ui.test.tsx`.

## Nhóm B — Khung trang

- [x] Bước 7 — `layout/AppShell.tsx`, `layout/NavTree.tsx` (mục 2, 7, 9, 22):
  - Link "Bỏ qua tới nội dung" là phần tử đầu tiên, chỉ hiện khi focus, nhảy tới `<main id="main" tabIndex={-1}>`.
  - Màn hẹp, ngăn kéo đóng: sidebar mang `inert`. Ngăn kéo mở: Esc đóng, Tab quay vòng trong sidebar và nút menu, đóng xong trả focus về nút menu.
  - Chuyển Đăng xuất vào menu tài khoản (mục cuối, màu `danger`, ngăn bằng một đường kẻ); bỏ nút khỏi sidebar.
  - Sidebar thu gọn: tên mục hiện thành nhãn nổi bên phải khi rê chuột và khi focus bàn phím; bỏ `title` trùng.
  - Cập nhật `tests/shared/layout/AppShell.test.tsx` cho vị trí Đăng xuất, skip link, `inert` và Esc.
- [x] Bước 8 — `themeBoot.ts`, `theme.ts` (mục 11): chưa có lựa chọn đã lưu thì theo `prefers-color-scheme`; khi hệ điều hành đổi mà người dùng chưa chọn thì đổi theo; bấm nút đổi theme thì lưu và ưu tiên lựa chọn đó. Thêm `tests/shared/theme.test.ts`.
- [x] Bước 9 — Tiêu đề tab (mục 3): thêm `export const metadata = { title }` cho từng `page.tsx` hoặc `layout.tsx` trong `frontend/src/app` theo tên màn hình trong menu (ví dụ "Đăng nhập", "Bài tập", "Luyện tập Quiz", "Tài khoản", "Nhật ký audit"); `not-found.tsx` có tiêu đề "Không tìm thấy trang". Trang chi tiết dùng tên mục cha (không tải tên lớp, tên bài để đặt tiêu đề). File `"use client"` thì đặt metadata ở `layout.tsx` cùng cấp.
- [x] Bước 10 — `ui/ErrorPage.tsx`, `styles/error-page.module.css` (mục 21): thêm nút đổi theme ở góc phải như màn đăng nhập; tiêu đề nét 700. Cập nhật `tests/shared/ui/ErrorPage.test.tsx`.

## Nhóm C — Màn hình theo unit

- [x] Bước 11 — U01 `features/identity/components/LoginForm.tsx` (mục 4): lỗi định dạng email hiện dưới ô Email, lỗi thiếu mật khẩu dưới ô Mật khẩu, ô lỗi có viền `danger`, `aria-invalid`, `aria-describedby` và nhận focus; lỗi từ máy chủ vẫn hiện chung phía trên nút. Cập nhật kiểm thử form đăng nhập hiện có trong `tests/features/identity`.
- [x] Bước 12 — U11 `features/quizpractice/pages/QuizTakingPage.tsx` (mục 5, 18):
  - Thanh tiêu đề/đồng hồ/Nộp bài `sticky` ngay dưới thanh trên của ứng dụng.
  - Vùng `aria-live` ẩn đọc "Còn 5 phút", "Còn 1 phút", "Còn 10 giây"; dưới 1 phút đồng hồ có thêm icon cảnh báo, không chỉ đổi màu.
  - Lưu lỗi: trạng thái chuyển màu `danger` kèm nút "Thử lại" gọi lại lần lưu.
  - Ô đáp án cao tối thiểu 44px, chữ 14px.
  - "Practice" đổi thành "Luyện tập" (cả chỗ còn lại do `grep` tìm ra).
  - Cập nhật `tests/features/quizpractice`.
- [x] Bước 13 — Bảng sinh viên trên điện thoại (mục 1, Câu 1 = A): dưới `sm`, mỗi dòng thành một thẻ xếp dọc (tên là link chính, các trường còn lại thành cặp nhãn–giá trị), từ `sm` trở lên giữ bảng như cũ. Áp dụng cho `features/assignmentwork/pages/StudentAssignmentsPage.tsx`, `features/quizpractice/pages/QuizPracticeHistoryPage.tsx` và bảng bài đã nộp trong `features/groupdocs/pages/GroupWorkspacePage.tsx`. Hai trang đầu dùng `TableSkeletonRows` khi đang tải. Cập nhật kiểm thử của ba trang.
- [x] Bước 14 — Bộ lọc dạng tab (mục 12): thay `role="tablist"` tự viết bằng `Segmented as="tab"` trong `ClassListPage`, `EnrollmentPanel`, `AiUsageHistoryPage`, `ClassTodoCard`, `StudentAssignmentsPage`, `GroupWorkspacePage`, `AccountListPage`, `BankQuestionPicker`, `QuestionListPage`, `SubjectQuizListPage`, `SubjectClassesTab`, `SubjectFilters`; giữ nguyên `data-testid`. `DocumentViewer` và `NotificationPopup` dùng CSS module riêng: chỉ thêm phím mũi tên và roving tabindex, không đổi giao diện.
- [x] Bước 15 — U10 `features/quizzes/components/AiDrafts.tsx`, `AiDraftTargets.tsx` (mục 13): thay 3 chỗ `window.confirm` bằng `Dialog` xác nhận (tiêu đề là câu hỏi, một nút "Bỏ chỉnh sửa" màu `danger`). Cập nhật kiểm thử liên quan trong `tests/features/quizzes`.
- [x] Bước 16 — Nút Hủy/Đóng (mục 19, Câu 7 = A): rà 12 chỗ đã liệt kê; bỏ nút chỉ gọi `onClose` của popup đã có nút X; giữ nút có việc riêng (`FileUploader` hủy tải, hủy sửa tại chỗ trong `ClassInfoForm`, `MyGroup`, `GradingWorkspacePage`, `QuizDetailParts` dạng `inline`). Cập nhật kiểm thử nào đang bấm các nút bị bỏ.
- [x] Bước 17 — Cỡ chữ (mục 17, Câu 6 = A): nâng mọi `text-[9px…11.5px]` trong 36 file và `font-size` dưới 12px trong ba CSS module lên 12px; kiểm tra lại các chỗ chật (mã môn trên sidebar, tiêu đề cột, số đếm trên chuông thông báo) để không vỡ bố cục.
- [x] Bước 18 — Mã màu (mục 20): thay hex viết cứng trong `shared/document/DocumentEditor.tsx`, `features/materials/components/MaterialPreview.tsx`, `DocumentViewer.tsx`, `features/ai/components/SetupGuides.tsx` bằng token; giữ nguyên `Logo.tsx`, `opengraph-image.tsx` và màu là dữ liệu của tài liệu người dùng (màu chữ, màu tô trong nội dung soạn thảo).

## Nhóm D — Tài liệu và kiểm tra

- [x] Bước 19 — `PRODUCT.md` (mục 10, Câu 3 = A; mục 9, 11): dòng Shell ghi thanh trên gồm thông báo, theme và tài khoản (đăng xuất nằm trong menu tài khoản), không có tìm kiếm; ghi theme mặc định theo hệ điều hành.
- [x] Bước 20 — Tóm tắt thay đổi tại `aidlc-docs/construction/ui-ux-remediation/code/summary.md`: file sửa, file tạo mới, theo từng mục 1–22.
- [x] Bước 21 — Build and Test: `npm run typecheck`, `npm run lint`, `npm test` trong `frontend/`; chạy dev server và kiểm tra trên trình duyệt các màn chưa đăng nhập ở hai theme và cỡ 375px (đăng nhập, kích hoạt, 404). Các màn sau đăng nhập chỉ kiểm tra được trên trình duyệt nếu người dùng đăng nhập sẵn trong khung trình duyệt; nếu không, ghi rõ là chỉ được kiểm thử tự động. Ghi kết quả vào cuối file này và `aidlc-state.md`.

## Extension

- Security Baseline (phạm vi đã duyệt): không đổi xác thực, phân quyền, CSP hay dữ liệu gửi đi; script theme vẫn chạy qua nonce. Các quy tắc còn lại N/A.
- Resiliency Baseline (phạm vi đã duyệt): nút "Thử lại" ở màn làm quiz dùng lại hàng đợi lưu hiện có, không thêm cơ chế gọi lại mới. Các quy tắc còn lại N/A.
- Property-Based Testing: tắt.

## Kết quả kiểm tra (2026-11-10)

- `npx tsc --noEmit`: không lỗi trong mã nguồn (chỉ còn lỗi ở `.next/types` cũ của route `receipt` đã bị xóa từ trước).
- `npx eslint .`: sạch, không cảnh báo.
- `npx vitest run`: 886/886 ca trong 128 file, ba lần chạy liên tiếp. Trước thay đổi là 867 ca; thêm 19 ca cho Toast, theme, Dialog, Menu, Segmented, bảng xếp thẻ, khung xương, AppShell, form đăng nhập, màn làm quiz và popup xác nhận.
- Trình duyệt (dev server, backend Docker local): tiêu đề tab "Đăng nhập · Nền tảng học tập" và "Không tìm thấy trang · Nền tảng học tập"; form đăng nhập để trống hiện lỗi dưới từng ô và focus vào ô Email; nút hiện mật khẩu 32×32px, hai link cao 24px; trang 404 ở 375px với hệ điều hành đặt tối tự dùng theme tối khi chưa lưu lựa chọn, tiêu đề nét 700, có nút đổi theme, không tràn ngang.
- Chưa kiểm tra trên trình duyệt: mọi màn sau đăng nhập (khung trang, ngăn kéo mobile, menu tài khoản, màn làm quiz, bảng xếp thẻ, popup xác nhận, toast). Các phần này chỉ có kiểm thử tự động vì phiên làm việc không đăng nhập được tài khoản thử.
- Chưa chạy `npm run build`.
- `node_modules` thiếu `xlsx`, `docx-preview`, `pptx-preview`, `pdfjs-dist` nên đã chạy `npm install`; `package-lock.json` chỉ bị đổi thứ tự và đã được hoàn nguyên.

## Điều chỉnh so với kế hoạch

- Bước 1: nhiều nơi dùng toast màu đỏ để báo đã xong thao tác xóa hay lưu trữ. Giữ các câu này tự ẩn; thêm `showErrorToast` (không tự ẩn, `role="alert"`) và chuyển 4 nơi báo lỗi thật sang hàm này (`AiDraftDialog`, `AssignmentFormPage`, `GradingWorkspacePage`, `moduleNav`). Bản đầu làm mọi toast đỏ không tự ẩn đã gây một ca kiểm thử chập chờn ở trang chi tiết môn học.
- Bước 2: thêm ngăn xếp popup để chỉ popup trên cùng xử lý Esc, Tab và giữ focus; cần cho popup xác nhận của bước 15 khi mở trong popup khác.
- Bước 5: dùng biến thể `pointer-coarse` có sẵn của Tailwind 4, không sửa `globals.css` cho phần này.
- Bước 14: `GroupWorkspacePage` dùng `Tabs` (kiểu gạch chân sẵn có) thay vì `Segmented` vì đây là tab chuyển nội dung trang, không phải bộ lọc.
- Bước 16: bỏ thêm nút Hủy trong popup Thêm sinh viên của `EnrollmentPanel`.
- Bước 17: chữ trong biểu đồ SVG của `DashboardCharts.tsx` giữ nguyên.
- Bước 18: dùng token cố định (không đổi theo theme) cho khung xem tài liệu, trang bảng tính và khối mã.
