# Mã số sinh viên (MSSV): kết quả Build and Test

- **Nhánh**: `fix/student-code`. Các thay đổi chưa được commit.
- **Ngày chạy**: 2026-11-14.
- **Phạm vi kiểm**: [kế hoạch code generation](../plans/code-generation-plan/2026-11-13-u01-student-code-code-generation-plan.md) và [tóm tắt mã](../u01-account-and-access/code/2026-11-13-student-code-summary.md).
- **Thu hẹp phạm vi ngày 2026-11-14**: đã bỏ chức năng gán MSSV cho tài khoản cũ (F16), cột MSSV vẫn được phép để trống.

## Kiểm tra tự động

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Backend | `mvn -o -B verify` | Qua: 2.366 test, 0 failure, 0 error, 0 skipped. Docker có chạy nên các test PostgreSQL đều được chạy, gồm 19 test của `AccountRepositoryIntegrationTest`: chặn trùng mã, check định dạng, check theo role, tìm theo mã, `updateAccountDetails` có kiểm `version`. Log: `.tmp/student-code-mvn-verify.log` |
| Frontend lint | `npm run lint` | Qua |
| Frontend type | `tsc --noEmit` | Không có lỗi trong mã nguồn |
| Frontend test | `vitest run` | 1042/1045 test qua. 3 test bị timeout 5 giây khi chạy cả bộ: 1 trong `authoringFlow` và 2 trong `bulk-selection`. Chạy lại riêng 2 file đó thì qua 18/18. Log: `.tmp/student-code-fe-vitest*.log` |
| Frontend build | `npm run build` | Qua, 30 trang tĩnh |

**Về lần build frontend đầu tiên:** build lỗi vì thư mục `.next/dev/types` còn type cũ trỏ tới những trang đã bị xóa (`admin/mcp`, `ai/usage`…). Đây là output build cũ, đã gitignore. Mình xóa `.next` rồi build lại thì qua. Mã nguồn không phải sửa gì.

## Thử trên trình duyệt (stack riêng)

**Cách dựng stack:**

- Dùng Docker Compose với project riêng tên `studentcode`, cấu hình trong `.tmp/studentcode/`.
- Image `platform-*:studentcode` được build từ thư mục làm việc của nhánh.
- Cổng: nginx 19088, backend 19080, PostgreSQL 15434, Mailpit 19025.
- Tệp env được chép từ `.env.local`, riêng admin khởi tạo dùng email thử `admin@studentcode.test`.
- Hai stack `platform` và `platform-main-qa` đang chạy không bị động tới.
- Lần `up` đầu báo RabbitMQ chưa healthy nên backend chưa bật. Chạy `up -d --wait` lần nữa thì mọi service đều healthy.

**Kiểm database của stack:** migration `20261113.1600` đã chạy thành công. Có đủ `accounts_student_code_format`, `accounts_student_code_role` và unique index `accounts_student_code_key`.

**Dữ liệu thử:**

- Tạo qua API với admin, Chủ nhiệm môn, giảng viên và 4 sinh viên.
- Kích hoạt tài khoản bằng OTP lấy từ Mailpit cục bộ.
- Mật khẩu thử được sinh ngẫu nhiên, lưu ở `.tmp/studentcode/credentials.txt`.
- Môn SWP391 và lớp SE1801 đang mở.

**Kiểm qua API:**

- Tạo sinh viên không có mã: báo `MISSING_STUDENT_CODE`.
- Mã sai định dạng: báo `INVALID_STUDENT_CODE`.
- Giảng viên có mã: báo `STUDENT_CODE_NOT_ALLOWED`.
- Trùng mã: báo `STUDENT_CODE_EXISTS`.
- Sinh viên đã kích hoạt mà đổi mã: báo `STUDENT_CODE_LOCKED`.
- Đổi role STUDENT sang TEACHER và TEACHER sang STUDENT: đều báo `STUDENT_ROLE_CHANGE_NOT_ALLOWED`.
- Sinh viên còn `PENDING`: sửa được mã. Nhập `se000022` được lưu thành `SE000022`.
- `GET /me/profile` của sinh viên trả `studentCode`.
- Endpoint `student-code-imports:validate` cũ trả 405, không còn xử lý.

**Kiểm trên trình duyệt trong app, màn 1280×800:**

- **Admin, danh sách tài khoản:**
  - Có cột MSSV.
  - Không còn nút "Gán MSSV từ Excel".
  - Tìm "se00001" ra đúng `SE000011`.
- **Admin, form Thêm tài khoản:**
  - Ô MSSV chỉ hiện khi chọn vai trò Sinh viên.
  - Nhập "S12" thì báo "MSSV gồm 2 chữ cái và 6 chữ số…".
  - Nhập "se000005" thì khi rời ô tự đổi thành `SE000005`, và tạo tài khoản thành công.
- **Admin, chi tiết sinh viên đã kích hoạt:**
  - MSSV `SE000011` hiện chỉ đọc, kèm câu "MSSV không đổi được sau khi sinh viên kích hoạt tài khoản."
  - Không có nút Đổi vai trò, có câu "Không đổi được vai trò của tài khoản sinh viên."
- **Admin, chi tiết sinh viên `PENDING`:**
  - Sửa mã thành "se000055" và lưu thì hiện toast "Đã lưu thay đổi", mã được lưu là `SE000055`.
- **Admin, popup Đổi vai trò của giảng viên:** chỉ có Giảng viên, Chủ nhiệm môn, Quản trị viên; không có Sinh viên và không có ô MSSV.
- **Chủ nhiệm môn, Thêm theo danh sách ở lớp SE1801:**
  - Dán `se000011`, một email, `SE000055`, `SE999999`, `se000011` lặp lại và `abc`.
  - Bước xem trước đánh dấu dòng lặp là "Trùng trong danh sách" và `abc` là "Không phải email hay MSSV hợp lệ".
  - Sau khi thêm, kết quả là 3 dòng "Đã thêm" (2 bằng MSSV, 1 bằng email) và `SE999999` báo "Không tìm thấy tài khoản".
  - Danh sách lớp có cột MSSV đứng trước Họ tên.
- **Sinh viên, trang Hồ sơ:** MSSV `SE000011` hiện chỉ đọc.
- **Console:** chỉ có lỗi 401 của các lần kiểm tra phiên khi chưa đăng nhập hoặc vừa đăng xuất, không có lỗi JavaScript.

## Thử bổ sung trên trình duyệt (người dùng yêu cầu thử nốt)

**Dữ liệu thêm:**

- Dựng qua API của stack bằng các script trong `.tmp/studentcode/`, dùng lại helper `scripts/e2e/lib.mjs`.
- Bài cá nhân GRADED "Bài cá nhân MSSV": đã hoàn tất và giao; sv1 và sv3 đã nộp.
- Quiz lớp có một câu: đã phát hành; sv1 đã làm một lượt.
- Nhóm A gồm sv1 (trưởng nhóm) và sv3.
- Sinh viên sv6 `SE000006`: tạo mới và ghi danh bằng danh sách "mssv / SE000006" qua API, kết quả `ENROLLED`.
- Đợt tự chia nhóm từ 2 đến 3 người: sv5 lập đề xuất "Nhóm B", mời sv6; sv6 chấp nhận, sv5 gửi duyệt.

**Kết quả:**

- **Bảng điểm dạng ma trận (giảng viên):** có cột MSSV cố định cùng STT và tên. Tìm "se000003" ra đúng một dòng.
- **Tệp xuất bảng điểm (lấy qua API):**
  - CSV có tiêu đề `STT, MSSV, Họ và tên, Email, …`.
  - XLSX: ô B2 kiểu văn bản `SE000011`; `pane xSplit="4" topLeftCell="E2"`.
- **Danh sách bài nộp ở tab Chấm điểm:** dòng phụ `SE000011 · email` dưới tên.
- **Lượt làm quiz (giảng viên):** cột MSSV cho cả 3 sinh viên.
- **Tab Sinh viên của giảng viên:**
  - MSSV hiện ở dòng phụ.
  - Lọc "se000055" ra đúng người.
  - Danh sách duyệt đề xuất "Nhóm B" hiện MSSV của cả hai thành viên.
- **Popup "Mời bạn" (sv5):**
  - Ứng viên hiện `SE000006 · email`.
  - Tìm "se000006" ra đúng người; tìm "se999" báo "Không có sinh viên phù hợp".
  - Gửi lời mời thành công.
- **"Nhóm của tôi":**
  - sv1 thấy MSSV của cả hai thành viên Nhóm A.
  - sv5 thấy đề xuất có `SE000055 · Trưởng nhóm` và `SE000006 · email`.
- **Màn hẹp 375px, chế độ tối:**
  - Trang nhóm của sv5 và bảng điểm của giảng viên không tràn ngang.
  - Bảng điểm gộp MSSV vào ô Sinh viên; ở 1280px vẫn có cột MSSV riêng.

**Lỗi tìm thấy khi thử, đã sửa và build lại image frontend của stack để kiểm:**

1. **Header trang chấm một bài (`GradingWorkspacePage`, `StudentPickerDropdown`)**
   - Lỗi: chỉ có tên và email, thiếu MSSV theo BR-U09-75.
   - Sửa: thêm `studentCode`, giờ hiện `SE000011 · email`.
   - Test: `workspace.test.tsx` kiểm thêm MSSV và tìm theo MSSV.
2. **Dòng thành viên đề xuất nhóm (`GroupProposalSection.PersonRow`)**
   - Lỗi: ghi chú ("Trưởng nhóm", "Đang chờ trả lời") thay chỗ dòng phụ nên mất MSSV.
   - Sửa: giờ hiện `SE000055 · Trưởng nhóm`.
3. **Bảng điểm ở màn hẹp (`GradebookMatrix`)**
   - Lỗi: 3 cột cố định (STT, MSSV, tên) chiếm gần hết 375px, cột điểm phải cuộn mới thấy.
   - Sửa: dưới `sm`, MSSV gộp vào ô Sinh viên theo quy ước chung ở frontend-components U01 mục 6.
   - Test: cập nhật `gradebook-matrix.test.tsx`.

**Kiểm lại sau khi sửa:**

- Frontend lint và tsc sạch.
- Vitest grading 194/194, nhóm 47/47.
- Toàn bộ Vitest 1039/1045: 6 test bị timeout 5 giây khi chạy cả bộ (authoringFlow, pagination, ClassAnnouncements, bulk-selection, SubjectDetailMutations). Chạy lại riêng 5 file đó thì qua 33/33.

**Còn lại chưa thử:**

- Thiết bị thật.
- Dòng phụ trong `GroupFormDialog` (popup tạo/sửa nhóm) và `PersonPicker` (ô chọn người) trên trình duyệt. Hai component này dùng chung `PersonSubline` và đã có test tự động.

## Tắt stack

Stack `studentcode` vẫn đang chạy để xem lại. Để tắt, chạy lệnh sau từ thư mục `infra/`:

```bash
docker compose -p studentcode -f docker-compose.yml -f docker-compose.local.yml -f ../.tmp/studentcode/compose.override.yml --env-file ../.tmp/studentcode/.env down -v
```

## Tuân thủ extension

- Security: SECURITY-03, 05, 08, 11 và 15 được giữ đúng như phần tuân thủ đã ghi ở functional design.
- Thử trên stack cho thấy backend chặn được các thao tác MSSV không hợp lệ, kể cả khi gọi thẳng API.
- Resiliency: migration chỉ thêm cột cho phép trống; các stack khác không bị ảnh hưởng.
- PBT: N/A vì đang tắt.

## Sau khi merge `origin/staging` (2026-11-14)

- **Merge:** staging có thêm 6 commit (đánh giá đóng góp thành viên nhóm, giữ hiển thị bài đã đóng mà chưa nộp).
  - Chỉ `aidlc-docs/audit.md` bị conflict, do cả hai bên cùng thêm mục vào cuối. Đã giữ đủ cả hai phần.
  - Các file grading cả hai bên cùng sửa thì tự gộp được.
- **Backend:** `mvn -o -B verify` qua 2.373 test, 0 lỗi. Log: `.tmp/student-code-mvn-verify-merged.log`.
- **Frontend:**
  - Lint và `tsc` sạch; build qua (30 trang).
  - Vitest qua 1062/1066. 4 test bị timeout 5 giây khi chạy cả bộ: authoringFlow, pagination, bulk-selection, và test mới từ staging trong workspace. Chạy lại riêng 4 file đó thì qua 64/64.
- **E2E:**
  - Sửa `scripts/e2e/seed.mjs` và `u04-u13-u14.mjs` để gửi kèm MSSV khi tạo sinh viên, vì backend giờ bắt buộc MSSV.
  - Dựng lại stack `studentcode` từ mã sau merge, gồm backend, worker và frontend.
  - Chạy `u04-u13-u14.mjs`: qua 40/40 ca. Log: `.tmp/e2e/student-code-u04-u13-u14.log`.
  - Thử nhanh MSSV trên stack đã dựng lại:
    - Thiếu mã thì báo `MISSING_STUDENT_CODE`; trùng mã thì báo `STUDENT_CODE_EXISTS`.
    - Tìm theo MSSV ra đúng tài khoản.
    - Hồ sơ của sv1 vẫn trả `SE000011`.
- **Chưa chạy:**
  - Các suite `u02`, `u06*` và `u12*`. Các suite này dùng `seed.json` của stack `localhost:8088`; stack đó đang được nhánh khác dùng nên không đụng tới.
  - Thiết bị thật.
