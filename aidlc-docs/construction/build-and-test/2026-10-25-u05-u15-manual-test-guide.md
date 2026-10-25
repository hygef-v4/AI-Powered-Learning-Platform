# Hướng dẫn kiểm thử thủ công U05 & U15 (Jira Acceptance Guide)

**Tài liệu:** Hướng dẫn kiểm thử thủ công và nghiệm thu người dùng (User Acceptance Test Guide)\
**Phạm vi:** U05 Quản lý Lớp học & Ghi danh (`UC20`–`UC30`, Jira Epic `KAN-21`) và U15 Bảng điều khiển Quản trị hệ thống (`UC66`, Jira Epic `KAN-32` / `KAN-80`)\
**Môi trường kiểm thử:**\
- Frontend: `http://localhost:3000`\
- Backend API: `http://localhost:8080/api/v1`\
- Nhánh Git: `mcp-test` tại commit `41d6cd19`\
**Quy tắc an toàn dữ liệu:**\
- **TUYỆT ĐỐI KHÔNG** sửa đổi, đổi giáo viên, xóa sinh viên, đổi trạng thái hoặc chỉnh sửa điểm trên lớp học mẫu chuẩn: `4d18e255-1789-45d6-8fbb-351e2188f672` (`U05-A`).\
- **TUYỆT ĐỐI KHÔNG** ghi đè hoặc chỉnh sửa các bài đánh giá mẫu: `7ca08c41-e9d4-4738-946d-24c049e3d1e5` và `826f5c29-b5d1-4a32-9999-9313c242d8d3`.\
- Chỉ thực hiện các thao tác tạo mới, ghi danh, gỡ ghi danh, đổi trạng thái trên môn học/lớp học tạm QA (ví dụ: lớp `U05TEST`, `QA-SKIP-20261023A` hoặc tạo mới lớp `QA-TEST-CLASS`).

---

## 1. Thiết lập tài khoản & vai trò

| Vai trò | Email đăng nhập | Quyền hạn & Phạm vi kiểm thử |
|---|---|---|
| **STUDENT** | `u05.student@school.test` | Xem danh sách lớp đã ghi danh, chi tiết lớp học sinh viên, xem tài liệu, bài tập, điểm cá nhân (`U05-01`, `U05-02`). |
| **TEACHER** | `u05.teacher@school.test` | Xem danh sách lớp được phân công, tổng quan lớp học, danh sách học viên, chi tiết sinh viên, sổ điểm (`U05-03`, `U05-04`). |
| **SUBJECT_MANAGER** | `u05.manager@school.test` | Quản lý môn học được phân công (`SWR392`), tạo lớp học, phân công giáo viên, quản lý sinh viên/ghi danh theo lô CSV, chuyển đổi trạng thái lớp (`U05-05` đến `U05-11`). |
| **ADMIN** | `admin@school.test` | Truy cập `/admin`, xem tổng quan số liệu tài khoản, môn học, lớp học, ghi danh, trạng thái sao lưu hệ thống (`U15-01` đến `U15-06`). |

---

## 2. Bảng ca kiểm thử thủ công (40 ca kiểm thử chi tiết)

> **Ghi chú:** Cột **Kết quả người dùng** được khởi tạo toàn bộ là `Chưa test`. Người dùng tự thực hiện kiểm thử thực tế trên trình duyệt và cập nhật trạng thái `Đạt` / `Không đạt` / `Bỏ qua`.

### Phần A: U05 - Dành cho Sinh viên (Student)

| Mã ca | Jira / UC | Vai trò | URL / Thao tác | Các bước thực hiện & Kết quả kỳ vọng | Dọn dẹp / Chú ý | Kết quả người dùng |
|---|---|---|---|---|---|---|
| **TC-U05-01** | `KAN-75` / `UC20` | STUDENT | `/classes` | 1. Đăng nhập tài khoản sinh viên.<br>2. Truy cập `/classes`.<br>3. Kiểm tra danh sách hiển thị các lớp `OPEN` đang ghi danh (lớp `U05-A`).<br>4. Xác nhận các lớp `DRAFT` (nháp) không xuất hiện. | Không tác động DB | `Chưa test` |
| **TC-U05-02** | `KAN-75` / `UC20` | STUDENT | `/classes` | 1. Tại ô tìm kiếm, nhập mã hoặc tên môn học (ví dụ: `U05` hoặc `SWR392`).<br>2. Kiểm tra danh sách lọc tức thì.<br>3. Xóa ô tìm kiếm, danh sách khôi phục đầy đủ. | Không tác động DB | `Chưa test` |
| **TC-U05-03** | `KAN-126` / `UC21` | STUDENT | `/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Bấm vào lớp `U05-A`.<br>2. Xác nhận hiển thị thông tin môn học, mã lớp, tên giáo viên phụ trách.<br>3. Chuyển qua các tab: Tổng quan, Tài liệu, Đánh giá/Bài tập.<br>4. Không có lỗi giao diện hoặc liên kết hỏng. | Không tác động DB | `Chưa test` |
| **TC-U05-04** | `KAN-126` / `UC21` | STUDENT | `/classes/00000000-0000-0000-0000-000000000000` | 1. Truy cập vào URL mã lớp không tồn tại hoặc lớp chưa được ghi danh.<br>2. Xác nhận hệ thống hiển thị thông báo lỗi 404/Không tìm thấy hoặc từ chối truy cập rõ ràng, không làm crash trang. | Không tác động DB | `Chưa test` |

---

### Phần B: U05 - Dành cho Giáo viên (Teacher)

| Mã ca | Jira / UC | Vai trò | URL / Thao tác | Các bước thực hiện & Kết quả kỳ vọng | Dọn dẹp / Chú ý | Kết quả người dùng |
|---|---|---|---|---|---|---|
| **TC-U05-05** | `KAN-51` / `UC22` | TEACHER | `/teaching/classes` | 1. Đăng nhập tài khoản giáo viên.<br>2. Truy cập `/teaching/classes`.<br>3. Xác nhận có 3 tab bộ lọc: `Đang dạy` (4 lớp OPEN), `Sắp mở` (1 lớp DRAFT), `Đã kết thúc` (0 lớp ARCHIVED).<br>4. Xác nhận không nhìn thấy lớp của giáo viên khác. | Không tác động DB | `Chưa test` |
| **TC-U05-06** | `KAN-51` / `UC22` | TEACHER | `/teaching/classes` | 1. Kiểm tra menu bên trái, mục `Lớp được phân công` phải được active highlight.<br>2. Bấm mục này; URL vẫn là `/teaching/classes`.<br>3. Trong Network, xác nhận thao tác không gọi `/api/v1/me/classes` rồi mới chuyển hướng. | Kiểm tra lại bản đã sửa Finding 3; không tác động DB | `Chưa test` |
| **TC-U05-07** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Mở chi tiết lớp `U05-A`.<br>2. Tab **Tổng quan**: Kiểm tra 4 thẻ thống kê đầu trang (Sinh viên: 1, Nhóm: 0, Đang mở: 0, Chờ chấm: 0).<br>3. Thẻ Metadata: hiển thị Mã lớp `U05-A`, Môn học `SWR392`, Học kỳ, Phổ điểm `Hiển thị`.<br>4. Kiểm tra card có viền 1px hiển thị rõ; phụ đề/nhãn phụ dùng màu xám dịu dễ đọc. | Kiểm tra lại bản đã sửa Finding 4; không tác động DB | `Chưa test` |
| **TC-U05-08** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Tại tab Tổng quan, cuộn xuống mục **Phổ điểm lớp học**.<br>2. Bấm mở rộng (Expand) phổ điểm.<br>3. Xác nhận biểu đồ/phổ điểm được tải đúng cách, không gửi request trùng lặp khi đang đóng. | Không tác động DB | `Chưa test` |
| **TC-U05-09** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Chuyển sang tab **Sinh viên**.<br>2. Kiểm tra danh sách hiển thị sinh viên `u05.student@school.test` với trạng thái `Đang học` (ACTIVE).<br>3. Kiểm tra ô tìm kiếm sinh viên xem có bị che khuất placeholder `"Tìm theo tên hoặc email"` không (đối chiếu Finding 5). | Không tác động DB | `Chưa test` |
| **TC-U05-10** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Bấm vào dòng sinh viên trong danh sách.<br>2. Modal `StudentDetailDialog` mở ra, hiển thị thông tin học viên và điểm số liên quan.<br>3. Nhấn phím `Escape` hoặc nút Đóng, modal đóng lại mượt mà, focus quay về bảng. | Không tác động DB | `Chưa test` |
| **TC-U05-11** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Xác nhận giao diện của giáo viên **hoàn toàn chỉ đọc** đối với thông tin lớp và danh sách sinh viên: không có nút xóa sinh viên, thêm sinh viên, đổi trạng thái lớp hay đổi giáo viên. | Không tác động DB | `Chưa test` |
| **TC-U05-12** | `KAN-135` / `UC23` | TEACHER | `/teaching/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Chuyển sang tab **Sổ điểm**.<br>2. Kiểm tra chế độ mặc định `Theo bài tập` hiển thị gọn gàng.<br>3. Bấm chuyển sang chế độ `Tổng hợp` (Matrix mode): cột sinh viên được cố định (sticky), các cột bài tập có độ rộng hợp lý, hiển thị đầy đủ điểm. | Không tác động DB | `Chưa test` |

---

### Phần C: U05 - Dành cho Trưởng bộ môn (Subject Manager)

| Mã ca | Jira / UC | Vai trò | URL / Thao tác | Các bước thực hiện & Kết quả kỳ vọng | Dọn dẹp / Chú ý | Kết quả người dùng |
|---|---|---|---|---|---|---|
| **TC-U05-13** | `KAN-141` / `UC30` | MANAGER | `/manager/subjects` | 1. Đăng nhập tài khoản Trưởng bộ môn.<br>2. Truy cập `/manager/subjects`.<br>3. Kiểm tra danh sách hiển thị môn học `SWR392` do mình quản lý với số lượng lớp thực tế. | Không tác động DB | `Chưa test` |
| **TC-U05-14** | `KAN-137` / `UC24` | MANAGER | `/manager/classes` | 1. Truy cập danh sách lớp học quản lý.<br>2. Kiểm tra bộ lọc theo môn học, trạng thái (`DRAFT`, `OPEN`, `ARCHIVED`) và thanh tìm kiếm.<br>3. Xác nhận chỉ thấy các lớp thuộc môn mình quản lý. | Không tác động DB | `Chưa test` |
| **TC-U05-15** | `KAN-50` / `UC26` | MANAGER | `/manager/classes` | 1. Bấm nút **Tạo lớp học**.<br>2. Form mở ra: nhập mã lớp `QA-TEST-01`, chọn môn `SWR392`, nhập học kỳ `FA26`.<br>3. Kiểm tra validation: để trống mã lớp -> báo lỗi; mã lớp quá 30 ký tự -> báo lỗi.<br>4. Lưu lớp mới -> Lớp tạo thành công ở trạng thái `DRAFT` (Bản nháp). | Sau khi test, chuyển lớp này về `ARCHIVED` | `Chưa test` |
| **TC-U05-16** | `KAN-139` / `UC27` | MANAGER | `/manager/classes/{id}` | 1. Vào lớp vừa tạo `QA-TEST-01`.<br>2. Bấm **Phân công giáo viên**.<br>3. Chọn giáo viên `u05.teacher@school.test` từ danh sách ứng viên.<br>4. Lưu -> Xác nhận giáo viên hiển thị chính xác trong thông tin lớp. | Áp dụng trên lớp QA | `Chưa test` |
| **TC-U05-17** | `KAN-52` / `UC28` | MANAGER | `/manager/classes/{id}` | 1. Tại tab Sinh viên của lớp QA, bấm **Thêm sinh viên**.<br>2. Tìm kiếm sinh viên `qa.u15.20261023@school.test` hoặc nhập email.<br>3. Thêm sinh viên thành công -> hiển thị trong danh sách. | Áp dụng trên lớp QA | `Chưa test` |
| **TC-U05-18** | `KAN-52` / `UC28` | MANAGER | `/manager/classes/{id}` | 1. Thử chức năng thêm sinh viên bằng dán danh sách email (Bulk Add).<br>2. Thử dán email hợp lệ và email sai định dạng.<br>3. Xác nhận hệ thống phân loại đúng email hợp lệ để thêm, và thông báo rõ dòng bị lỗi. | Giới hạn danh sách <= 200 dòng | `Chưa test` |
| **TC-U05-19** | `KAN-52` / `UC28` | MANAGER | `/manager/classes/{id}` | 1. Chọn một sinh viên vừa thêm vào lớp QA, bấm **Xóa khỏi lớp** (Remove).<br>2. Hộp thoại xác nhận hiển thị cảnh báo.<br>3. Xác nhận xóa -> Sinh viên chuyển trạng thái `REMOVED`.<br>4. Bấm **Khôi phục** (Restore) -> Sinh viên quay lại trạng thái `ACTIVE`. | Áp dụng trên lớp QA | `Chưa test` |
| **TC-U05-20** | `KAN-140` / `UC29` | MANAGER | `/manager/classes/{id}` | 1. Tại lớp QA đang ở `DRAFT` và đã có giáo viên, bấm **Mở lớp** (DRAFT -> OPEN).<br>2. Xác nhận trạng thái chuyển sang `OPEN`.<br>3. Bật tùy chọn **Hiển thị phổ điểm cho sinh viên** (`showGradeDistribution`).<br>4. Lưu và tải lại trang, xác nhận cờ cài đặt được lưu bền vững. | Áp dụng trên lớp QA | `Chưa test` |
| **TC-U05-21** | `KAN-140` / `UC29` | MANAGER | `/manager/classes/{id}` | 1. Bấm **Đóng lớp** (OPEN -> ARCHIVED) trên lớp QA.<br>2. Xác nhận trạng thái chuyển thành `ARCHIVED`.<br>3. Xác nhận toàn bộ giao diện lớp sau khi ARCHIVED chuyển sang chế độ chỉ đọc. | Áp dụng trên lớp QA | `Chưa test` |

---

### Phần D: U15 - Dành cho Quản trị viên hệ thống (Admin Dashboard)

| Mã ca | Jira / UC | Vai trò | URL / Thao tác | Các bước thực hiện & Kết quả kỳ vọng | Dọn dẹp / Chú ý | Kết quả người dùng |
|---|---|---|---|---|---|---|
| **TC-U15-01** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Đăng nhập tài khoản Quản trị viên (`admin@school.test`).<br>2. Truy cập `/admin`.<br>3. Xác nhận hiển thị Dashboard với thời gian tính toán (`calculatedAt`). | Không tác động DB | `Chưa test` |
| **TC-U15-02** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Kiểm tra khối thống kê **Tài khoản**: hiển thị số lượng theo vai trò (ADMIN, TEACHER, MANAGER, STUDENT) và trạng thái (ACTIVE, PENDING, DISABLED).<br>2. Xác nhận số lượng khớp với thực tế dữ liệu. | Không tác động DB | `Chưa test` |
| **TC-U15-03** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Kiểm tra khối thống kê **Học thuật**: số lượng Môn học (`ACTIVE`, `ARCHIVED`), Lớp học (`DRAFT`, `OPEN`, `ARCHIVED`), và tổng số lượt ghi danh `ACTIVE`. | Không tác động DB | `Chưa test` |
| **TC-U15-04** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Kiểm tra thẻ **Trạng thái sao lưu (Backup Status)**.<br>2. Hiển thị rõ: Cấu hình tự động (`Đang tắt` hoặc `Bật`), giờ chạy hàng ngày, kết quả lần chạy gần nhất, số lần thất bại trong 7 ngày qua.<br>3. Không hiển thị các nút thao tác sao lưu thủ công hoặc khôi phục dữ liệu trái phép tại đây. | Không bấm chạy backup | `Chưa test` |
| **TC-U15-05** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Bấm nút xem **Lịch sử sao lưu** (Backup History).<br>2. Hộp thoại mở ra hiển thị danh sách các lần chạy sao lưu kèm phân trang.<br>3. Nếu chưa có lần chạy nào, hiển thị thông báo trống lịch thiệp, không bị vỡ giao diện. | Không tác động DB | `Chưa test` |
| **TC-U15-06** | `KAN-80` / `UC66` | ADMIN | `/admin` | 1. Bấm nút **Làm mới dữ liệu** (Refresh) trên đầu trang.<br>2. Xác nhận icon quay nhẹ và số liệu được cập nhật lại mới nhất mà không phải tải lại toàn bộ trang trình duyệt. | Không tác động DB | `Chưa test` |

---

### Phần E: Kiểm tra Bảo mật & Phân quyền chéo (Cross-Role Security)

| Mã ca | Jira / UC | Vai trò | URL / Thao tác | Các bước thực hiện & Kết quả kỳ vọng | Dọn dẹp / Chú ý | Kết quả người dùng |
|---|---|---|---|---|---|---|
| **TC-SEC-01** | `KAN-80` / `UC66` | TEACHER | `/admin` | 1. Khi đang đăng nhập tài khoản Giáo viên, gõ URL `/admin` trên thanh địa chỉ.<br>2. Xác nhận hệ thống chặn lại và hiển thị thông báo từ chối truy cập (403 Forbidden). | Không tác động DB | `Chưa test` |
| **TC-SEC-02** | `KAN-80` / `UC66` | STUDENT | `/admin` | 1. Khi đang đăng nhập Sinh viên, truy cập `/admin`.<br>2. Xác nhận hệ thống chặn lại và từ chối truy cập. | Không tác động DB | `Chưa test` |
| **TC-SEC-03** | `KAN-137` / `UC24` | TEACHER | `/manager/classes` | 1. Đăng nhập Giáo viên, truy cập đường dẫn quản lý môn `/manager/classes`.<br>2. Xác nhận trang hiển thị lỗi từ chối quyền (403 ROLE_DENIED). | Không tác động DB | `Chưa test` |
| **TC-SEC-04** | `KAN-126` / `UC21` | TEACHER | `/classes/4d18e255-1789-45d6-8fbb-351e2188f672` | 1. Đăng nhập Giáo viên, truy cập vào đường dẫn xem lớp của Sinh viên `/classes/{id}`.<br>2. Xác nhận hiển thị màn hình từ chối quyền (`u05-02-teacher-on-student-class-denied.png`). | Không tác động DB | `Chưa test` |
| **TC-SEC-05** | `KAN-80` / `UC66` | TEACHER | `GET /api/v1/admin/dashboard` | 1. Gọi trực tiếp API bằng Postman/Curl hoặc fetch với session Giáo viên.<br>2. Xác nhận nhận mã lỗi `403 DASHBOARD_FORBIDDEN`.<br>3. Kiểm tra trường hợp query sai (`?page=abc`) để đối chiếu Finding 1 trong báo cáo. | Không tác động DB | `Chưa test` |

---

## 3. Quy trình xác nhận kết quả & Bước tiếp theo

1. Người dùng lần lượt đăng nhập từng vai trò trên trình duyệt.
2. Thực hiện các bước thao tác theo từng ca kiểm thử trong bảng trên.
3. Ghi nhận kết quả vào cột **Kết quả người dùng**:
   - Ghi `Đạt` nếu giao diện và phản hồi đúng như kỳ vọng.
   - Ghi `Không đạt` kèm mô tả chi tiết nếu phát hiện lỗi mới.
   - Ghi `Bỏ qua` nếu chưa đủ điều kiện dữ liệu để thử.
4. Sau khi kiểm thử xong, nếu người dùng muốn sửa chữa các lỗi đã phát hiện trong Báo cáo Audit (Findings 1 đến 8), hãy yêu cầu triển khai kế hoạch sửa mã nguồn (Code Fix Plan).
