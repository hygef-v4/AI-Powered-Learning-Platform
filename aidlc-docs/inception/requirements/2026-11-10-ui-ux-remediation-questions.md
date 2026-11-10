# Sửa 22 điểm UI/UX — yêu cầu và câu hỏi xác nhận

Nhánh làm việc: `fix/ui-ux-remediation`, tạo từ `staging` (`062a43f5`). Chưa sửa mã nguồn.

## Yêu cầu đã ghi nhận

Người dùng yêu cầu sửa toàn bộ 22 điểm của đợt rà soát UI/UX ngày 2026-11-10.

- **Loại yêu cầu**: Enhancement và Bug Fix (accessibility, mobile, tính nhất quán).
- **Phạm vi**: chỉ `frontend/` (khung trang, component dùng chung, các trang của nhiều unit) và `PRODUCT.md` nếu Câu 3 chọn A. Không đổi backend, API, schema.
- **Độ phức tạp**: vừa. Nhiều thay đổi nhỏ trải rộng; riêng mục 1 (bảng trên điện thoại) chạm nhiều trang.
- **Chuẩn áp dụng**: WCAG 2.2 AA, cam kết trong `PRODUCT.md`, [quy ước mã nguồn](../../construction/code-conventions.md).

| # | Mức | Nội dung sửa | Cần quyết định |
|---|---|---|---|
| 1 | Cao | Bảng không tràn ngang trên điện thoại: chuyển thành thẻ xếp dọc ở màn hẹp | Câu 1 |
| 2 | Cao | Ngăn kéo menu mobile: ẩn khỏi thứ tự Tab khi đóng, bẫy focus và đóng bằng Esc khi mở | |
| 3 | Cao | Mỗi trang có tiêu đề tab riêng theo mẫu `%s · Nền tảng học tập` | |
| 4 | Cao | Form đăng nhập: lỗi nằm dưới đúng ô, ô lỗi được tô viền và nhận focus | |
| 5 | Cao | Màn làm quiz: thanh đồng hồ/Nộp bài dính trên đầu, báo mốc thời gian cho trình đọc màn hình, nút thử lại khi lưu lỗi, ô đáp án cao tối thiểu 44px | |
| 6 | Cao | Toast: lỗi không tự ẩn, có nút đóng, dừng đếm khi rê chuột/focus, lỗi dùng `role="alert"` | |
| 7 | TB | Link "Bỏ qua tới nội dung" ở đầu khung trang | |
| 8 | TB | Vùng chạm tối thiểu 24px (nút hiện mật khẩu, link đăng nhập); phân trang, số câu quiz, nút icon, công tắc đạt 44px trên màn cảm ứng | |
| 9 | TB | Vị trí nút Đăng xuất | Câu 2 |
| 10 | TB | Ô tìm kiếm ở thanh trên | Câu 3 |
| 11 | TB | Theme mặc định khi người dùng chưa chọn | Câu 4 |
| 12 | TB | 14 bộ lọc dạng tab tự viết chuyển sang `Segmented`; `Segmented` hỗ trợ phím mũi tên và roving tabindex | |
| 13 | TB | 3 chỗ `window.confirm` (đề xuất AI của quiz) chuyển sang `Dialog` | |
| 14 | TB | `Menu`: điều hướng bằng mũi tên/Home/End, `aria-haspopup`, link tài khoản có `role="menuitem"` | |
| 15 | TB | `Dialog`: khóa cuộn trang nền; tiêu đề popup cỡ `md` giảm từ 26px xuống 22px | |
| 16 | TB | Trạng thái đang tải | Câu 5 |
| 17 | Thấp | Cỡ chữ tối thiểu | Câu 6 |
| 18 | Thấp | Nhãn "Practice" ở màn làm quiz đổi sang tiếng Việt ("Luyện tập") | |
| 19 | Thấp | Nút Hủy/Đóng trùng với nút X của popup | Câu 7 |
| 20 | Thấp | Mã màu viết cứng trong `DocumentEditor`, `MaterialPreview`, `SetupGuides` chuyển sang token; giữ nguyên màu thương hiệu của logo và ảnh Open Graph | |
| 21 | Thấp | Trang 404/lỗi: thêm nút đổi theme, tiêu đề dùng nét đậm như các trang khác | |
| 22 | Thấp | Sidebar thu gọn: tên mục hiện khi rê chuột và khi focus bàn phím (không chỉ `title`) | |

## Đề xuất các giai đoạn AI-DLC

- **Chạy**: Workspace Detection (xong: brownfield, tài liệu reverse engineering hiện có được dùng lại), Requirements Analysis (tài liệu này), Workflow Planning (mục này), Code Generation (kế hoạch có checkbox rồi mới sửa mã), Build and Test (typecheck, lint, Vitest, kiểm tra trên trình duyệt ở hai theme và cỡ điện thoại).
- **Bỏ qua**: Reverse Engineering (đã có), User Stories (không thêm chức năng người dùng mới), Application Design và Units Generation (không thêm component nghiệp vụ hay unit), Functional Design, NFR Requirements/Design, Infrastructure Design (không có logic nghiệp vụ, hạ tầng hay công nghệ mới).
- **Extension**: giữ cấu hình hiện có. Security Baseline và Resiliency Baseline bật theo phạm vi đã duyệt; Property-Based Testing tắt.

## Question 1
Mục 1: 23 bảng đang đặt bề rộng tối thiểu 480–980px. Chuyển sang dạng thẻ trên điện thoại cho những bảng nào?

A) Chỉ các bảng sinh viên dùng (Bài tập, lịch sử luyện quiz, tài liệu nhóm, quiz trong lớp); bảng của giảng viên, chủ nhiệm môn, admin giữ cuộn ngang. Đề xuất, vì `PRODUCT.md` chỉ yêu cầu sinh viên dùng tốt trên điện thoại.

B) Toàn bộ 23 bảng.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Mục 9: nút Đăng xuất đặt ở đâu?

A) Chuyển vào menu tài khoản ở thanh trên, bỏ khỏi sidebar. Đề xuất, khớp cam kết "sidebar chỉ chứa link trang".

B) Giữ trong sidebar và thêm một mục nữa trong menu tài khoản.

C) Giữ nguyên như hiện tại.

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Mục 10: `PRODUCT.md` ghi thanh trên có tìm kiếm, nhưng yêu cầu và backend hiện không có chức năng tìm kiếm toàn cục.

A) Không thêm tìm kiếm; sửa `PRODUCT.md` để thanh trên chỉ còn thông báo, theme và tài khoản. Đề xuất, vì `PRODUCT.md` cũng cấm tự thêm chức năng ngoài đặc tả.

B) Thêm ô tìm kiếm chỉ để nhảy nhanh tới các mục menu của vai trò hiện tại (không gọi backend).

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Mục 11: khi người dùng chưa từng chọn theme thì dùng giao diện nào? Mã hiện tại cố ý mặc định sáng.

A) Theo cài đặt sáng/tối của hệ điều hành; sau khi người dùng bấm nút đổi theme thì theo lựa chọn đó. Đề xuất.

B) Giữ mặc định sáng.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Mục 16: trạng thái đang tải.

A) Giữ logo bút mài nghiên cho lần tải cả trang; thêm khung xương (skeleton) cho bảng và danh sách đang tải bên trong trang. Đề xuất.

B) Như A, và thêm `loading.tsx` cho mọi nhóm route.

C) Giữ nguyên như hiện tại.

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
Mục 17: cỡ chữ.

A) Nâng mọi chữ dưới 12px lên 12px; giữ chữ nền 14px. Đề xuất.

B) Như A, và nâng chữ nền lên 15px trên màn hẹp.

C) Giữ nguyên như hiện tại.

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Mục 19: khoảng 12 nút Hủy/Đóng trong popup.

A) Bỏ những nút chỉ đóng popup (trùng nút X); giữ nút có việc riêng như hủy tải file hay hủy sửa tại chỗ. Đề xuất, khớp `PRODUCT.md`.

B) Giữ nguyên tất cả.

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Cách bàn giao sau khi sửa và kiểm tra xong.

A) Để nguyên thay đổi trên nhánh `fix/ui-ux-remediation`, không commit, để bạn xem diff trước. Đề xuất.

B) Commit theo từng nhóm trên nhánh, không push.

C) Commit, push và mở pull request vào `staging`.

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Các giai đoạn AI-DLC đề xuất ở trên.

A) Đồng ý; lập kế hoạch Code Generation để duyệt rồi mới sửa mã.

B) Đồng ý và duyệt luôn việc sửa mã: lập kế hoạch có checkbox rồi thực hiện ngay, không dừng chờ duyệt kế hoạch.

C) Thêm giai đoạn User Stories.

D) Other (please describe after [Answer]: tag below)

[Answer]: A
