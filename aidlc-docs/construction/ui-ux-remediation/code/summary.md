# Sửa 22 điểm UI/UX — tóm tắt thay đổi

Nhánh `fix/ui-ux-remediation`, 2026-11-10. Chỉ sửa `frontend/` và `PRODUCT.md`; không đổi backend, API, schema hay `data-testid` sẵn có. [Yêu cầu và quyết định](../../../inception/requirements/2026-11-10-ui-ux-remediation-questions.md); [kế hoạch và kết quả kiểm tra](../../plans/code-generation-plan/2026-11-10-ui-ux-remediation.md).

## File tạo mới

- `frontend/src/shared/ui/Skeleton.tsx`: `Skeleton`, `TableSkeletonRows`.
- `frontend/src/shared/ui/ConfirmDialog.tsx`: `useConfirmDialog`.
- `frontend/src/shared/ui/tabKeys.ts`: `handleTabListKey`.
- `frontend/tests/shared/ui/Toast.test.tsx`, `frontend/tests/shared/theme.test.ts`.

## Thay đổi theo từng mục

| # | Thay đổi | File chính |
|---|---|---|
| 1 | `TableFrame stackOnMobile` và `Td label`/`hideWhenStacked`: dưới 640px mỗi dòng là một thẻ. Áp dụng cho Bài tập của sinh viên, lịch sử luyện quiz, bảng bản nộp của tài liệu nhóm | `shared/ui/Table.tsx`, `shared/styles/globals.css`, `StudentAssignmentsPage.tsx`, `QuizPracticeHistoryPage.tsx`, `GroupWorkspacePage.tsx` |
| 2 | Ngăn kéo mobile: `inert` khi đóng; mở thì Tab quay vòng giữa nút menu và sidebar, Esc đóng và trả focus | `shared/layout/AppShell.tsx` |
| 3 | 52 trang có `metadata.title` riêng theo mẫu `%s · Nền tảng học tập` | `src/app/**/page.tsx`, `not-found.tsx` |
| 4 | Lỗi nằm dưới đúng ô, `aria-invalid`, `aria-describedby`, focus vào ô lỗi đầu tiên | `LoginForm.tsx`, `PasswordField.tsx` |
| 5 | Thanh đồng hồ `sticky`; vùng `aria-live` đọc mốc 5 phút, 1 phút, 10 giây; icon cảnh báo dưới 1 phút; nút Thử lại khi lưu lỗi; ô đáp án cao 44px | `QuizTakingPage.tsx` |
| 6 | Mọi toast có nút đóng, dừng đếm khi rê chuột/focus. `showErrorToast` cho lỗi: không tự ẩn, `role="alert"` | `shared/ui/Toast.tsx` và 4 nơi báo lỗi |
| 7 | Link "Bỏ qua tới nội dung" tới `<main id="main">` | `AppShell.tsx` |
| 8 | Vùng chạm 44px trên màn cảm ứng (`pointer-coarse`) cho nút icon, phân trang, `Segmented`, `Switch`, nút đóng popup, số câu quiz; nút hiện mật khẩu 32px; link đăng nhập cao 24px | `Button.tsx`, `Pagination.tsx`, `PageNav.tsx`, `inputs.tsx`, `Dialog.tsx`, `QuestionPager.tsx`, `SecretInput.tsx` |
| 9 | Đăng xuất chuyển vào menu tài khoản, bỏ khỏi sidebar | `AppShell.tsx` |
| 10 | Không thêm tìm kiếm; sửa cam kết Shell | `PRODUCT.md` |
| 11 | Chưa chọn theme thì theo hệ điều hành, kể cả khi hệ điều hành đổi lúc trang đang mở | `themeBoot.ts`, `theme.ts`, `PRODUCT.md` |
| 12 | `Segmented`: roving tabindex, mũi tên/Home/End, `count`, `testId`. 10 bộ lọc chuyển sang `Segmented`, tab Tài liệu/Bản nộp sang `Tabs`; `DocumentViewer` và `NotificationPopup` thêm phím mũi tên | `inputs.tsx` và 13 file trong `features/` |
| 13 | 3 chỗ `window.confirm` chuyển sang popup xác nhận | `AiDrafts.tsx`, `AiDraftTargets.tsx` |
| 14 | `Menu`: `aria-haspopup`, `aria-expanded`, mũi tên/Home/End, Esc trả focus; link tài khoản có `role="menuitem"` | `Menu.tsx`, `AppShell.tsx` |
| 15 | Khóa cuộn trang nền; tiêu đề cỡ `md` 22px; chỉ popup trên cùng xử lý Esc, Tab và giữ focus (cần cho popup xác nhận lồng trong popup) | `Dialog.tsx` |
| 16 | Khung xương cho bảng Bài tập và lịch sử luyện quiz khi đang tải | `Skeleton.tsx` |
| 17 | Mọi `text-[9–11.5px]` và `font-size` dưới 12px trong CSS module lên 12px (39 file); số đếm trên chuông thông báo 18px | nhiều file |
| 18 | "Practice" thành "Luyện tập" ở màn làm quiz và lịch sử luyện quiz | `QuizTakingPage.tsx`, `QuizPracticeHistoryPage.tsx` |
| 19 | Bỏ 7 nút Hủy/Đóng chỉ để đóng popup | `AccountDialogs.tsx`, `EnrollmentPanel.tsx`, `TeacherQuizSections.tsx`, `DrawRulesPanel.tsx`, `QuizDetailParts.tsx`, `QuestionListPage.tsx` |
| 20 | Mã màu viết cứng thành token đặt tên (`viewer`, `sheet-*`, `code-*`) | `globals.css`, `DocumentViewer.tsx`, `MaterialPreview.tsx`, `SetupGuides.tsx` |
| 21 | Trang lỗi có nút đổi theme; tiêu đề nét 700 | `ErrorPage.tsx`, `error-page.module.css` |
| 22 | Sidebar thu gọn: nhãn nổi khi rê chuột và khi focus bàn phím, bỏ `title` | `AppShell.tsx` |

## Khác với kế hoạch

- Mục 6: nhiều nơi dùng toast màu đỏ để báo đã xong thao tác xóa hay lưu trữ ("Đã xóa thông báo"). Những câu này vẫn tự ẩn; chỉ lỗi thật (gọi `showErrorToast`) mới ở lại.
- Mục 8: Tailwind 4 đã có sẵn biến thể `pointer-coarse`, không cần thêm vào `globals.css`.
- Mục 17: chữ trong biểu đồ SVG của Admin Dashboard (`DashboardCharts.tsx`, `fontSize` 10–11) giữ nguyên vì chưa xem được bố cục biểu đồ trên trình duyệt.
- Mục 20: các màu này phải giống nhau ở cả hai theme (khung xem tài liệu nền tối, trang bảng tính nền trắng, khối mã), nên được đặt tên thành token cố định thay vì dùng token đổi theo theme. Bảng màu chữ và màu tô trong `DocumentEditor.tsx` là dữ liệu của tài liệu, giữ nguyên.

## Ngoài phạm vi, chưa sửa

- `features/quizzes/hooks/useAuthoringGuard.ts` vẫn dùng `window.confirm` để chặn rời trang khi đang soạn dở; đổi sang popup cần đổi luồng điều hướng đồng bộ thành bất đồng bộ.
- 20 bảng của giảng viên, chủ nhiệm môn và admin vẫn cuộn ngang trên màn hẹp (Câu 1 = A).
- `gradingLabels` của giảng viên vẫn ghi "Graded"/"Practice" như thuật ngữ nghiệp vụ.
