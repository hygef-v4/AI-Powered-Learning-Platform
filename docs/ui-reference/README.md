# UI Reference — Nền tảng học tập

Bản xuất offline của canvas thiết kế (Claude Design, version 35, 18/10/2026): 45 màn theo screen flow G21, 123 artboard gồm màn chính, tab, bước, popup và trạng thái. Đây là chuẩn giao diện chung khi code frontend. Màn nào code ra cũng phải giống artboard tương ứng ở đây.

Canvas gốc (để comment, sửa): https://claude.ai/artifact/M5RgKNauBqdxM7GUTRwau4

## Cách dùng

1. Mở `index.html` bằng trình duyệt (bấm đúp). Lọc theo unit của mình hoặc tìm theo số màn, tên màn, tên file.
2. Bấm một thẻ để mở màn ở khổ 1280 px. Mỗi màn là một file HTML tĩnh trong `screens/`, dùng CSS trong `css/`; chỉ font cần mạng.
3. Muốn lấy số đo, màu, khoảng cách: mở DevTools (F12) và soi trực tiếp phần tử.
4. Bảng màn → unit → người code ở cuối file này.

Không chép nguyên HTML vào code. HTML ở đây là bản vẽ (class Tailwind v3 của tool thiết kế, nhiều style inline). Code thật dùng component trong `frontend/src/shared` và token trong `frontend/src/app/globals.css` như hướng dẫn dưới.

## Ghép thiết kế với code

### Khung trang

| Trên thiết kế | Trong code |
|---|---|
| Header (logo, Kết nối AI, chuông, avatar, Đăng xuất) và sidebar theo vai | `shared/layout/AppShell` — unit không tự vẽ header/sidebar |
| Mục trên sidebar | Đăng ký `NavEntry` trong `features/<unit>/register.ts` (xem `features/audit/register.ts`) |
| Chuông thông báo trên header | Gắn qua slot `HeaderWidget` (U14) |
| Popup xác nhận Đăng xuất | `shared/layout/LogoutDialog` |
| Phần nội dung giữa (`<main>`) | Trang của unit: `app/<route>/page.tsx` gọi component trong `features/<unit>/` |

Admin không có mục "Kết nối AI" trên header.

### Component dùng chung (`@/shared/ui`)

| Trên thiết kế | Component |
|---|---|
| Tiêu đề trang, breadcrumb, nút quay lại, nút thao tác bên phải | `PageHeader` |
| Thẻ trắng bo góc có tiêu đề (card) | `Section` |
| Nút xanh / nút viền trắng / nút đỏ / nút chữ | `Button` / `ButtonLink` với `variant` = `primary` / `secondary` / `danger` / `ghost`, `size` = `md` / `sm` |
| Nhãn trạng thái bo tròn (Đang mở, Nháp, Thành công…) | `Pill` với `tone` |
| Ô icon vuông có nền nhạt | `IconTile` |
| Hộp báo xanh / vàng / đỏ | `Alert` |
| Popup | `Dialog` |
| Bảng | `TableFrame`, `Th`, `Td`, `ClickableRow` |
| Ô nhập, ô chọn, công tắc, nhóm chip lọc | `Field`, `TextInput`, `Select`, `Switch`, `ChipToggleGroup` |
| Icon | `Icon` (thiết kế dùng Material Symbols; tên icon giữ nguyên) |

Thiếu component (phân trang, tabs, toast…) thì báo người phụ trách frontend để thêm vào `shared/ui`, không tự viết riêng trong unit.

### Màu (đã khai báo trong `globals.css`, dùng qua class Tailwind)

| Vai trò | Token | Mã |
|---|---|---|
| Màu chính (nút, link, tab đang chọn) | `primary` / `primary-strong` / `primary-soft` | #2F45B5 / #24369A / #EAEDFB |
| Chữ đậm / chữ thường / chữ phụ | `ink` / `body` / `muted` | #0F172A / #475069 / #64708A |
| Viền | `line` / `line-strong` / `line-soft` | #E8EBF2 / #D9DEEA / #F1F3F8 |
| Nền trang / nền khung / đầu bảng | `canvas` / `shell` / `table-head` | #F7F9FE / #F4F6FB / #F7F8FC |
| Thành công / Cảnh báo / Lỗi / Thông tin / Nhấn | `success` / `warning` / `danger` / `info` / `accent` (mỗi loại có `-soft`) | #1C7A4A / #8A5A00 / #B42B2B / #1A7FA6 / #7A3E8F |

Font: Be Vietnam Pro. Tiêu đề trang 24 px đậm; chữ nội dung 13 px; nhãn bảng 11 px in hoa.

### Trạng thái (badge và bộ lọc phải đủ bộ)

| Đối tượng | Trạng thái → tone của `Pill` |
|---|---|
| Bài tập | Nháp `neutral`, Đã duyệt `info`, Đã lên lịch `accent`, Đang mở `success`, Đã đóng `primary`, Ngưng giao `danger` |
| Quiz | Nháp `neutral`, Đã duyệt `info`, Đang mở `success`, Ngưng `danger` |
| Câu hỏi ngân hàng | Nháp `neutral`, Đang dùng `success`, Ngưng `danger` |
| Lớp | Nháp `neutral`, Đang mở `success`, Lưu trữ `primary` |
| Môn | Hoạt động `success`, Lưu trữ `primary` |
| Học liệu | Đang dùng `success`, Lưu trữ `primary` (không có Xóa) |
| Tài khoản | Hoạt động `success`, Chờ kích hoạt `warning`, Vô hiệu hóa `danger` |

## Quy tắc đã chốt trên thiết kế

- Tài khoản chỉ có email, họ tên, số điện thoại, vai trò, trạng thái. Không có MSSV, khoa, học vị. Chỗ cần phân biệt người thì hiện email.
- Học liệu là một danh sách có thứ tự, không có Module. Học liệu được lưu trữ, không bị xóa.
- Quiz chỉ có chế độ Practice. Bài nhóm luôn Graded và chỉ có ở bài của lớp.
- Mọi nút gọi AI mở popup "Chọn nguồn AI" (artboard `c16-ai-source-popup`): API của trường (chỉ giảng viên, chủ nhiệm môn) hoặc API cá nhân, hiện bên chịu chi phí, hạn mức còn lại, số token ước tính. Agent MCP không phải nguồn của yêu cầu AI.
- Khóa API và token agent chỉ hiện 4 ký tự cuối.
- Thao tác không hoàn tác được (xóa, lưu trữ, gỡ, công bố, chốt điểm, đăng xuất) đều có popup xác nhận.
- Không hiện dữ liệu mà hệ thống không lưu (ví dụ ngày kích hoạt, ngày mở lớp, ghi chú mức quota).
- Thiết kế chỉ vẽ trạng thái có dữ liệu. Khi code phải làm thêm: đang tải, danh sách rỗng, lỗi tải, lỗi nhập dưới ô, không có quyền (403).

## Khi thiết kế và tài liệu thiết kế unit khác nhau

Tài liệu thiết kế unit (`aidlc-docs/construction/<unit>/functional-design/`) là chuẩn cho dữ liệu và nghiệp vụ; UI reference là chuẩn cho bố cục và hình thức. Thấy lệch (thiếu trường, thừa trường, sai trạng thái) thì báo nhóm để sửa canvas, không tự đoán.

## Bảng màn

Mỗi người code màn của unit mình; màn dùng chung (khung trang, popup chọn nguồn AI) do unit ghi trong bảng làm, unit khác dùng lại.

### Chung

| Artboard | Màn | Unit | Người code |
|---|---|---|---|
| [Main](screens/Main.html) | #1 · User Login — Đăng nhập | U01 | Người 1 |
| [c02a-activation-email](screens/c02a-activation-email.html) | #2 · Activate Account — Bước 1: nhập email | U01 | Người 1 |
| [c02b-activation-otp](screens/c02b-activation-otp.html) | #2 · Activate Account — Bước 2: nhập mã OTP | U01 | Người 1 |
| [c02-account-activation](screens/c02-account-activation.html) | #2 · Activate Account — Bước 3: đặt mật khẩu | U01 | Người 1 |
| [c03a-recovery-email](screens/c03a-recovery-email.html) | #3 · Password Reset — Bước 1: nhập email | U01 | Người 1 |
| [c03-password-recovery](screens/c03-password-recovery.html) | #3 · Password Reset — Bước 2: nhập mã OTP | U01 | Người 1 |
| [c04-password-reset](screens/c04-password-reset.html) | #3 · Password Reset — Bước 3: đặt mật khẩu mới | U01 | Người 1 |
| [c05-profile](screens/c05-profile.html) | #4 · User Profile — Hồ sơ | U01 | Người 1 |
| [c06-change-password-popup](screens/c06-change-password-popup.html) | #5 · Password Change (popup) — Đổi mật khẩu | U01 | Người 1 |
| [c12-ai-connection](screens/c12-ai-connection.html) | #6 · AI Connection — Kết nối AI | U06 | Người 5 |
| [c12a-add-api-key-popup](screens/c12a-add-api-key-popup.html) | #6 · AI Connection — Thêm khóa API (popup) | U06 | Người 5 |
| [c12b-new-agent-popup](screens/c12b-new-agent-popup.html) | #6 · AI Connection — Kết nối agent mới (popup) | U06 | Người 5 |
| [c12c-agent-token-popup](screens/c12c-agent-token-popup.html) | #6 · AI Connection — Token agent hiện một lần (popup) | U06 | Người 5 |
| [c13-ai-usage-history](screens/c13-ai-usage-history.html) | #7 · AI Usage History — Lịch sử sử dụng AI | U06 | Người 5 |
| [c14-notifications-popup](screens/c14-notifications-popup.html) | Thanh trên cùng · Notifications (popup) — Thông báo | U14 | Người 3 |
| [c15-logout-popup](screens/c15-logout-popup.html) | Thanh trên cùng · Log out (popup) — Xác nhận đăng xuất | U01 | Người 1 |
| [c16-ai-source-popup](screens/c16-ai-source-popup.html) | Dùng chung · Choose AI source (popup) — Chọn nguồn AI | U06 | Người 5 |

### Quản trị viên

| Artboard | Màn | Unit | Người code |
|---|---|---|---|
| [a01-statistic](screens/a01-statistic.html) | #8 · Admin Dashboard — Tổng quan hệ thống | U15 | Người 5 |
| [a01b-backup-history-popup](screens/a01b-backup-history-popup.html) | #8 · Admin Dashboard — Lịch sử backup (popup) | U15 | Người 5 |
| [a15-setting-list](screens/a15-setting-list.html) | #9 · Setting List — Cài đặt | U03 | Người 3 |
| [a14b-setting-detail-value](screens/a14b-setting-detail-value.html) | #10 · Setting Detail — Mục khóa–giá trị | U03 | Người 3 |
| [a14-settings](screens/a14-settings.html) | #10 · Setting Detail — API AI của trường và hạn mức | U03 | Người 3 |
| [a14c-ai-usage-tab](screens/a14c-ai-usage-tab.html) | #10 · Setting Detail — API AI của trường · tab Usage | U03 | Người 3 |
| [a02-account-list](screens/a02-account-list.html) | #11 · Account List — Danh sách tài khoản | U01 | Người 1 |
| [a02b-account-csv-import](screens/a02b-account-csv-import.html) | #11 · Account List — Nhập CSV | U01 | Người 1 |
| [a03-new-account-popup](screens/a03-new-account-popup.html) | #12 · New Account (popup) — Thêm tài khoản | U01 | Người 1 |
| [a04-account-detail](screens/a04-account-detail.html) | #13 · Account Detail — Chi tiết tài khoản | U01 | Người 1 |
| [a04b-change-role-popup](screens/a04b-change-role-popup.html) | #13 · Account Detail — Đổi vai trò (popup) | U01 | Người 1 |
| [a04c-change-role-rejected-popup](screens/a04c-change-role-rejected-popup.html) | #13 · Account Detail — Đổi vai trò bị từ chối (popup) | U01 | Người 1 |
| [a04d-deactivate-popup](screens/a04d-deactivate-popup.html) | #13 · Account Detail — Vô hiệu hóa / Mở lại (popup) | U01 | Người 1 |
| [a13-audit-log](screens/a13-audit-log.html) | #14 · Audit Log — Nhật ký audit | U02 | Người 2 |
| [a13b-audit-detail-popup](screens/a13b-audit-detail-popup.html) | #14 · Audit Log — Chi tiết bản ghi (popup) | U02 | Người 2 |
| [a13c-audit-actor-not-found](screens/a13c-audit-actor-not-found.html) | #14 · Audit Log — Email người thực hiện không khớp | U02 | Người 2 |
| [a05-subject-list](screens/a05-subject-list.html) | #15 · Subject List — Danh sách môn học | U04 | Người 4 |
| [a06-add-subject-popup](screens/a06-add-subject-popup.html) | #15 · Subject List — Thêm môn học (popup) | U04 | Người 4 |
| [a07-subject-detail](screens/a07-subject-detail.html) | #16 · Subject Detail — Chi tiết môn học | U04 | Người 4 |
| [a07b-subject-classes-tab](screens/a07b-subject-classes-tab.html) | #16 · Subject Detail — Tab Lớp học | U04 | Người 4 |
| [a07c-change-manager-popup](screens/a07c-change-manager-popup.html) | #16 · Subject Detail — Đổi / bỏ chủ nhiệm (popup) | U04 | Người 4 |
| [a07d-archive-rejected-popup](screens/a07d-archive-rejected-popup.html) | #16 · Subject Detail — Lưu trữ bị từ chối (popup) | U04 | Người 4 |

### Chủ nhiệm môn

| Artboard | Màn | Unit | Người code |
|---|---|---|---|
| [m11-manager-dashboard](screens/m11-manager-dashboard.html) | #17 · Manager Dashboard — Tổng quan môn phụ trách | U04 | Người 4 |
| [m01-subject-detail](screens/m01-subject-detail.html) | #18 · My Subject — Môn học của tôi | U04 | Người 4 |
| [m07-subject-question-bank](screens/m07-subject-question-bank.html) | #19 · Question List — Ngân hàng câu hỏi trắc nghiệm | U10 | Người 1 |
| [m07b-question-import-popup](screens/m07b-question-import-popup.html) | #19 · Question List — Nhập từ tệp (popup) | U10 | Người 1 |
| [m07c-question-ai-draft-popup](screens/m07c-question-ai-draft-popup.html) | #19 · Question List — Tạo nháp bằng AI (popup) | U10 | Người 1 |
| [m08-question-editor](screens/m08-question-editor.html) | #20 · Question Detail — Chi tiết câu hỏi | U10 | Người 1 |
| [m12-quiz-list](screens/m12-quiz-list.html) | #21 · Quiz List — Danh sách Quiz | U10 | Người 1 |
| [t10-quiz-editor](screens/t10-quiz-editor.html) | #22 · Quiz Detail — Chi tiết Quiz | U10 | Người 1 |
| [t10b-quiz-bank-picker-popup](screens/t10b-quiz-bank-picker-popup.html) | #22 · Quiz Detail — Lấy từ ngân hàng: chọn tay (popup) | U10 | Người 1 |
| [t10d-quiz-bank-random-popup](screens/t10d-quiz-bank-random-popup.html) | #22 · Quiz Detail — Lấy từ ngân hàng: bốc ngẫu nhiên (popup) | U10 | Người 1 |
| [m13-material-list](screens/m13-material-list.html) | #23 · Material List — Học liệu môn | U13 | Người 4 |
| [m13b-material-add-menu](screens/m13b-material-add-menu.html) | #23 · Material List — Thêm học liệu: Tải tệp / Link YouTube | U13 | Người 4 |
| [m02-learning-material](screens/m02-learning-material.html) | #24 · Material Detail — Chi tiết học liệu | U13 | Người 4 |
| [m02b-material-archive-popup](screens/m02b-material-archive-popup.html) | #24 · Material Detail — Lưu trữ học liệu (popup) | U13 | Người 4 |
| [t09-assignment-list](screens/t09-assignment-list.html) | #25 · Assignment List — Danh sách bài tập | U07 | Người 4 |
| [t13-diagram-essay-editor](screens/t13-diagram-essay-editor.html) | #26 · Assignment Form — Bài cá nhân · Bước 1: Nội dung | U07 | Người 4 |
| [t13c-assignment-schedule](screens/t13c-assignment-schedule.html) | #26 · Assignment Form — Bài cá nhân · Bước 3: Lịch & lượt nộp | U07 | Người 4 |
| [t13d-assignment-preview](screens/t13d-assignment-preview.html) | #26 · Assignment Form — Bài cá nhân · Bước 4: Xem trước và thao tác | U07 | Người 4 |
| [t13b-assignment-ai-draft-popup](screens/t13b-assignment-ai-draft-popup.html) | #26 · Assignment Form — Tạo nháp bằng AI (popup) | U07 | Người 4 |
| [t13e-assignment-import-docx-popup](screens/t13e-assignment-import-docx-popup.html) | #26 · Assignment Form — Nhập DOCX (popup) | U07 | Người 4 |
| [t15-group-essay-editor](screens/t15-group-essay-editor.html) | #26 · Assignment Form — Bài nhóm (bài của lớp) | U12 | Người 2 |
| [t14-rubric-popup](screens/t14-rubric-popup.html) | #27 · Rubric Detail (popup) — Chi tiết rubric | U07 | Người 4 |
| [m03-subject-classes](screens/m03-subject-classes.html) | #28 · Class List — Lớp của môn | U05 | Người 5 |
| [m04-create-class-popup](screens/m04-create-class-popup.html) | #28 · Class List — Tạo lớp (popup) | U05 | Người 5 |
| [m05-class-detail](screens/m05-class-detail.html) | #29 · Class Detail — Chi tiết lớp | U05 | Người 5 |
| [m05b-class-students-tab](screens/m05b-class-students-tab.html) | #29 · Class Detail — Tab Sinh viên | U05 | Người 5 |
| [m05d-remove-student-popup](screens/m05d-remove-student-popup.html) | #29 · Class Detail — Gỡ sinh viên (popup) | U05 | Người 5 |
| [m06-assign-teacher-popup](screens/m06-assign-teacher-popup.html) | #29 · Class Detail — Phân công giảng viên (popup) | U05 | Người 5 |
| [m05c-reopen-rejected-popup](screens/m05c-reopen-rejected-popup.html) | #29 · Class Detail — Mở lại bị từ chối (popup) | U05 | Người 5 |

### Giảng viên

| Artboard | Màn | Unit | Người code |
|---|---|---|---|
| [t10c-quiz-attempts-teacher](screens/t10c-quiz-attempts-teacher.html) | #22 · Quiz Detail — Lượt làm của sinh viên (giảng viên) | U11 | Người 1 |
| [t01-assigned-classes](screens/t01-assigned-classes.html) | #30 · Class Dashboard — Giảng viên · Lớp được phân công | U05 | Người 5 |
| [t08-class-announcements](screens/t08-class-announcements.html) | #31 · Class Announcements — Giảng viên · Thông báo lớp | U14 | Người 3 |
| [t08b-announcement-edit-popup](screens/t08b-announcement-edit-popup.html) | #31 · Class Announcements — Sửa thông báo (popup) | U14 | Người 3 |
| [t08c-announcement-delete-popup](screens/t08c-announcement-delete-popup.html) | #31 · Class Announcements — Xóa thông báo (popup) | U14 | Người 3 |
| [t18-class-info](screens/t18-class-info.html) | #32 · Teacher Class Detail — Tab Thông tin lớp | U05 | Người 5 |
| [t20-class-materials](screens/t20-class-materials.html) | #32 · Teacher Class Detail — Tab Học liệu | U13 | Người 4 |
| [t02c-class-students-ungrouped](screens/t02c-class-students-ungrouped.html) | #32 · Teacher Class Detail — Tab Sinh viên · chưa chia nhóm | U12 | Người 2 |
| [t02d-random-split-popup](screens/t02d-random-split-popup.html) | #32 · Teacher Class Detail — Chia ngẫu nhiên (popup) | U12 | Người 2 |
| [t02-class-detail](screens/t02-class-detail.html) | #32 · Teacher Class Detail — Tab Sinh viên · đã chia nhóm | U12 | Người 2 |
| [t02f-group-editor](screens/t02f-group-editor.html) | #32 · Teacher Class Detail — Tab Sinh viên · sửa nhóm | U12 | Người 2 |
| [t02e-group-progress](screens/t02e-group-progress.html) | #32 · Teacher Class Detail — Tab Sinh viên · tiến độ bài nhóm | U12 | Người 2 |
| [t02h-release-section-popup](screens/t02h-release-section-popup.html) | #32 · Teacher Class Detail — Nhả mục (popup) | U12 | Người 2 |
| [t21-class-evals](screens/t21-class-evals.html) | #32 · Teacher Class Detail — Tab Đánh giá | U07 | Người 4 |
| [t21b-create-assignment-popup](screens/t21b-create-assignment-popup.html) | #32 · Teacher Class Detail — Tạo bài tập (popup) | U07 | Người 4 |
| [t02b-gradebook](screens/t02b-gradebook.html) | #32 · Teacher Class Detail — Tab Sổ điểm | U09 | Người 2 |
| [t02j-gradebook-export-popup](screens/t02j-gradebook-export-popup.html) | #32 · Teacher Class Detail — Xuất sổ điểm (popup) | U09 | Người 2 |
| [t19-student-detail-popup](screens/t19-student-detail-popup.html) | #33 · Student Detail (popup) — Chi tiết sinh viên | U09 | Người 2 |
| [t16-student-submissions](screens/t16-student-submissions.html) | #34 · Submission Detail — Bài nộp của sinh viên | U09 | Người 2 |
| [t16b-ai-batch-popup](screens/t16b-ai-batch-popup.html) | #34 · Submission Detail — AI đề xuất hàng loạt (popup) | U09 | Người 2 |
| [t16c-publish-confirm-popup](screens/t16c-publish-confirm-popup.html) | #34 · Submission Detail — Chốt / Công bố điểm (popup) | U09 | Người 2 |
| [t17-grading-workspace](screens/t17-grading-workspace.html) | #35 · Grading Workspace — Bài cá nhân | U09 | Người 2 |
| [t17b-grading-group](screens/t17b-grading-group.html) | #35 · Grading Workspace — Bài nhóm | U12 | Người 2 |

### Sinh viên

| Artboard | Màn | Unit | Người code |
|---|---|---|---|
| [s01-my-classes](screens/s01-my-classes.html) | #30 · Class Dashboard — Sinh viên · Lớp của tôi | U05 | Người 5 |
| [s06-class-announcements](screens/s06-class-announcements.html) | #31 · Class Announcements — Sinh viên · Thông báo lớp | U14 | Người 3 |
| [s02-class-detail](screens/s02-class-detail.html) | #36 · Student Class Detail — Chi tiết lớp | U05 | Người 5 |
| [s03-my-group](screens/s03-my-group.html) | #36 · Student Class Detail — Nhóm của tôi | U12 | Người 2 |
| [s04-leader-change-popup](screens/s04-leader-change-popup.html) | #36 · Student Class Detail — Yêu cầu đổi trưởng nhóm (popup) | U12 | Người 2 |
| [s05-learning-material](screens/s05-learning-material.html) | #37 · Learning Material — Học liệu dạng tệp | U13 | Người 4 |
| [s05b-learning-material-youtube](screens/s05b-learning-material-youtube.html) | #37 · Learning Material — Video YouTube | U13 | Người 4 |
| [s19-student-assignments](screens/s19-student-assignments.html) | #38 · Student Assignments — Bài tập | U08 | Người 3 |
| [s12-assignment-detail](screens/s12-assignment-detail.html) | #39 · Assignment Detail — Bài Graded đang làm | U08 | Người 3 |
| [s12b-assignment-detail-practice](screens/s12b-assignment-detail-practice.html) | #39 · Assignment Detail — Bài Practice | U08 | Người 3 |
| [s12c-assignment-detail-published](screens/s12c-assignment-detail-published.html) | #39 · Assignment Detail — Bài Graded đã công bố điểm | U08 | Người 3 |
| [s12d-assignment-detail-group](screens/s12d-assignment-detail-group.html) | #39 · Assignment Detail — Bài nhóm | U12 | Người 2 |
| [s16-diagram-essay-workspace](screens/s16-diagram-essay-workspace.html) | #40 · Assignment Workspace — Bài cá nhân · Draw.io | U08 | Người 3 |
| [s16b-submit-popup](screens/s16b-submit-popup.html) | #40 · Assignment Workspace — Nộp bài cá nhân (popup) | U08 | Người 3 |
| [s16c-submission-receipt](screens/s16c-submission-receipt.html) | #40 · Assignment Workspace — Biên nhận sau khi nộp | U08 | Người 3 |
| [s18-group-essay-workspace](screens/s18-group-essay-workspace.html) | #40 · Assignment Workspace — Bài nhóm · thành viên | U12 | Người 2 |
| [s18f-group-workspace-leader](screens/s18f-group-workspace-leader.html) | #40 · Assignment Workspace — Bài nhóm · trưởng nhóm giao mục | U12 | Người 2 |
| [s18b-section-editor-popup](screens/s18b-section-editor-popup.html) | #40 · Assignment Workspace — Trình soạn mục toàn trang | U12 | Người 2 |
| [s18c-group-submit-popup](screens/s18c-group-submit-popup.html) | #40 · Assignment Workspace — Nộp tài liệu nhóm (popup) | U12 | Người 2 |
| [s18d-group-submission-card](screens/s18d-group-submission-card.html) | #40 · Assignment Workspace — Thẻ Bản nộp | U12 | Người 2 |
| [s18e-group-auto-submit](screens/s18e-group-auto-submit.html) | #40 · Assignment Workspace — Tự nộp khi hết hạn | U12 | Người 2 |
| [s13-submission-history](screens/s13-submission-history.html) | #41 · Submission History — Bài Practice | U08 | Người 3 |
| [s13b-submission-history-graded](screens/s13b-submission-history-graded.html) | #41 · Submission History — Bài Graded đã chấm | U08 | Người 3 |
| [s13c-ai-feedback-popup](screens/s13c-ai-feedback-popup.html) | #41 · Submission History — Nhận góp ý AI (popup) | U08 | Người 3 |
| [s20-quiz-practice-history](screens/s20-quiz-practice-history.html) | #42 · Quiz Practice History — Luyện tập Quiz | U11 | Người 1 |
| [s21-quiz-practice-detail](screens/s21-quiz-practice-detail.html) | #43 · Quiz Practice Detail — Chi tiết luyện tập | U11 | Người 1 |
| [s21b-quiz-practice-unavailable](screens/s21b-quiz-practice-unavailable.html) | #43 · Quiz Practice Detail — Hết lượt làm | U11 | Người 1 |
| [s14-quiz-workspace](screens/s14-quiz-workspace.html) | #44 · Quiz Taking — Làm Quiz | U11 | Người 1 |
| [s14b-quiz-submit-popup](screens/s14b-quiz-submit-popup.html) | #44 · Quiz Taking — Nộp bài (popup) | U11 | Người 1 |
| [s14c-quiz-time-up](screens/s14c-quiz-time-up.html) | #44 · Quiz Taking — Hết giờ tự nộp | U11 | Người 1 |
| [s22-quiz-result](screens/s22-quiz-result.html) | #45 · Quiz Result — Kết quả Quiz | U11 | Người 1 |
| [s22b-quiz-result-hidden](screens/s22b-quiz-result-hidden.html) | #45 · Quiz Result — Quiz không hiện điểm | U11 | Người 1 |
