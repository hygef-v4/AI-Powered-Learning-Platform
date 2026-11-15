# Học kỳ của lớp (D-U04-05): kết quả Build and Test

- **Nhánh**: `fix/term`, các thay đổi chưa được commit.
- **Ngày chạy**: 2026-11-15.
- **Phạm vi kiểm**: [kế hoạch code generation](../plans/code-generation-plan/2026-11-14-u05-class-term-code-generation-plan.md) và [tóm tắt mã](../u05-class-enrollment/code/2026-11-14-class-term-summary.md).
- **Cách build và chạy test**: theo [build-instructions.md](build-instructions.md), [unit-test-instructions.md](unit-test-instructions.md) và [integration-test-instructions.md](integration-test-instructions.md); lệnh không đổi. Không có yêu cầu hiệu năng mới (NFR-CT không đặt ngưỡng), nên không chạy [performance test](performance-test-instructions.md).

## Kiểm tra tự động

| Hạng mục | Lệnh | Kết quả |
|---|---|---|
| Backend | `mvn -o -B verify` | Qua sau 18 phút 39 giây: 2.426 test, 0 failure, 0 error, 0 skipped. Docker có chạy nên mọi test PostgreSQL đều được chạy, gồm `ClassStartDateMigrationIntegrationTest` (11), `ClassTermMigrationIntegrationTest` (2), `AcademicsPersistenceTest` (7). Log: `.tmp/classterm/mvn-verify.log` |
| Frontend lint | `npm run lint` | Qua |
| Frontend type | `tsc --noEmit` | Không lỗi |
| Frontend test | `vitest run` | 1.082/1.092 test qua. 10 test bị timeout 5 giây khi chạy cả bộ, trong lúc `mvn verify` cũng đang chạy (máy bị tải nặng): `authoringFlow`, `enrollmentSelection` (2), `pagination.characterization`, `ClassAnnouncementsPage`, `bulk-selection` (3), `grading/workspace`, `SubjectDetailMutations`, `OtpFlow`. Chạy lại riêng 8 file đó: 80/83 qua, 3 test còn timeout; chạy 3 test này với `--testTimeout=60000` thì qua 59/59. Riêng `pagination.characterization` (file có fixture mới) chạy một mình mất 2,3 giây, dưới mức 5 giây. Log: `.tmp/classterm/fe-vitest*.log` |
| Frontend build | `npm run build` | Qua |

## Thử trên stack riêng `classterm`

**Cách dựng stack:**

- Dùng Docker Compose với project riêng tên `classterm`, cấu hình trong `.tmp/classterm/`.
- Image `platform-*:classterm` được build từ thư mục làm việc của nhánh. Database mới, không đụng các stack `platform`, `platform-main-qa`, `studentcode`.
- Cổng: nginx 29088, backend 29080, PostgreSQL 25434, Mailpit 29025.
- Lần `up` đầu báo RabbitMQ chưa healthy. Lần thứ hai worker khởi động chậm (105 giây) nên quá hạn healthcheck; restart worker thì healthy.

**Database:** Flyway ở phiên bản `20261114.1100`. Có `ck_course_classes_starts_on`, `ck_course_classes_term`, khóa `uq_course_classes_subject_term_code`, index `ix_course_classes_subject_term`; cột `term` là cột generated stored (`attgenerated = s`).

**Kiểm qua API** bằng `.tmp/classterm/seed.mjs` (dùng helper `scripts/e2e/lib.mjs`, mật khẩu thử sinh ngẫu nhiên ở `.tmp/classterm/credentials.txt`): **21/21 qua**.

- Tạo `SE1801` ngày 2026-09-07 → `FA26`, `SE1801` ngày 2027-04-03 → `SP27-BL5` (cùng mã, khác học kỳ), `SE1802` ngày 2027-01-10 → `SP27`.
- Tạo `SE1801` ngày 2026-10-01 (cùng `FA26`): 409 `CLASS_EXISTS`.
- Ngày 1999-12-31: 400 `INVALID_CLASS_START_DATE`. Thiếu `startsOn` hoặc gửi `term`: 400 `INVALID_REQUEST`.
- Mở 3 lớp, ghi danh `sv1` vào `SE1801 FA26`, `sv2` vào `SE1801 SP27-BL5`.
- `GET /manager/classes?term=SP27-BL5`: đúng 1 lớp. `?term=sp27`: 400 `INVALID_CLASS_TERM`. Giảng viên `?term=FA26`: chỉ `SE1801 FA26`.
- Danh sách giảng viên theo thứ tự `SE1801/SP27-BL5, SE1801/FA26, SE1802/SP27`.
- Sửa ngày `SE1801 FA26` sang 2027-04-10 (đụng `SE1801 SP27-BL5`): 409 `CLASS_EXISTS`.
- `GET /admin/subjects/{id}/classes` trả `term` cho cả 3 lớp.

**Kiểm trên trình duyệt trong app** (khung 1024×768):

- **Chủ nhiệm môn, Lớp học:**
  - Bảng có cột Học kỳ; ô "Tất cả học kỳ" liệt kê `SP27-BL5`, `SP27`, `FA26`.
  - Chọn `SP27` thì còn `SE1802`, URL có `classTerm=SP27`.
- **Chủ nhiệm môn, popup Tạo lớp:**
  - Ngày bắt đầu để trống; bấm Tạo lớp thì báo "Chọn ngày bắt đầu".
  - Chọn 1/10/2026 hiện "Học kỳ: FA26"; tạo `SE1801` báo "Mã lớp đã tồn tại trong môn ở học kỳ này" ngay dưới ô Mã lớp.
  - Đổi sang 20/8/2027 hiện "Học kỳ: SU27-BL5"; tạo được, danh sách có `SE1801 SU27-BL5` đứng trước `SP27-BL5` và `FA26`.
- **Chủ nhiệm môn, Chi tiết lớp `SE1802`:**
  - Tiêu đề "SE1802 SP27"; thẻ Thông tin lớp có Ngày bắt đầu và Học kỳ chỉ đọc.
  - Đổi ngày sang 3/4/2027 hiện "Học kỳ: SP27 → SP27-BL5"; lưu xong tiêu đề và ô Học kỳ thành `SP27-BL5`, không có lỗi.
- **Giảng viên, Lớp được phân công** (`?classTerm=SP27-BL5`):
  - Tab "Đang dạy 2", "Kết thúc 0"; thẻ ghi "SWP391 · SP27-BL5".
  - Ô chọn lớp trên sidebar ghi "SWP391 · SP27-BL5 / SE1801", "SWP391 · FA26 / SE1801", "SWP391 · SP27-BL5 / SE1802".
  - Chi tiết lớp: Môn học, Mã lớp, Học kỳ FA26, Sinh viên, Trạng thái.
- **Sinh viên, Thông tin lớp:** Ngày bắt đầu 7/9/2026, Học kỳ FA26; sidebar "SWP391 · FA26".
- Console chỉ có lỗi HTTP dự kiến (401 khi chưa đăng nhập, 409 của lần thử trùng mã, 403 khi trang thử quyền giảng viên); không có lỗi script.

**Kiểm bổ sung trên trình duyệt** (2026-11-15, theo yêu cầu "kiểm trên trình duyệt và e2e"):

- **Dữ liệu thêm** (`.tmp/classterm/fixture-assignments.mjs`, `fixture-publish.mjs`): giảng viên tạo, hoàn tất và phát hành bài luyện tập "Báo cáo tuần 1" ở `SE1801 FA26` và `SE1802 SP27-BL5`. Ghi danh `sv1` vào `SE1802` bị từ chối `IN_OTHER_CLASS` (một sinh viên chỉ ở một lớp của môn), đúng quy tắc hiện có.
- **Admin, Môn học → tab Lớp học:** cột Học kỳ; 4 lớp `SE1801 SU27-BL5`, `SE1801 SP27-BL5`, `SE1801 FA26`, `SE1802 SP27-BL5`.
- **Giảng viên, popup Tạo thông báo** (trang Tất cả thông báo): ô Chọn lớp liệt kê "SWP391 · SP27-BL5 / SE1801", "SWP391 · FA26 / SE1801", "SWP391 · SP27-BL5 / SE1802". Popup mở từ trang thông báo của một lớp không có ô chọn lớp (lớp cố định), đúng thiết kế.
- **Sinh viên, trang Bài tập:** ô lọc lớp có "SWP391 · FA26 / SE1801"; bảng có bài "Báo cáo tuần 1 (FA26)".
- **Giảng viên, popup Bài tập mới → Đề có sẵn** (ở lớp `SE1801 SP27-BL5`): "Báo cáo tuần 1 (FA26) · SE1801 · FA26" và "Báo cáo tuần 1 (SE1802) · SE1802 · SP27-BL5". Đóng popup, không tạo bài.
- Console chỉ có lỗi HTTP dự kiến (401 sau khi đăng xuất, 403/404 khi thử đường dẫn sai vai trò); không có lỗi script.

## Script e2e trên stack `classterm`

Chạy lần lượt sau `node scripts/e2e/seed.mjs` (tạo `SE-TEST1`, 5 sinh viên), với `E2E_BASE_URL=http://localhost:29088`, `E2E_MAILPIT_URL=http://localhost:29025`, `U01_BOOTSTRAP_ADMIN_EMAIL=admin@classterm.test` và mật khẩu thử ở `credentials.txt`. Log: `.tmp/classterm/e2e-<bộ>.log`.

| Bộ | Kết quả | Nguyên nhân lỗi |
|---|---|---|
| `u04-u13-u14` (script có sửa để gửi `startsOn`) | **40/40 qua** | — |
| `u02` | 15/16 | "U06/U12 ghi audit qua U02" cần có sẵn sự kiện MCP/AI/nhóm trong audit; bộ này chạy trước U06/U12 trên database mới nên chưa có. Phụ thuộc thứ tự chạy |
| `u12-notify` | 3/3 qua | — |
| `u06` | 24/25 | "U06-11 giảng viên thấy hạn mức": stack không bật API AI của trường (`API của trường chưa được bật`) |
| `u12`, `u12b` | dừng giữa chừng | Phát hành bài nhóm trả 409 `ASSIGNMENT_NOT_FINALIZED`: script (sửa lần cuối 2026-11-08) không gọi bước Hoàn tất, bước này bắt buộc từ commit `bc490784` (2026-11-11). Các bước sau lỗi theo |
| `u12c` | dừng ở bước chuẩn bị | Cùng nguyên nhân `ASSIGNMENT_NOT_FINALIZED` |
| `u06b` | dừng ở bước đầu | Kết nối AI Gemini của chủ nhiệm môn không lưu được trên stack này (`conn("GEMINI")` rỗng); module AI không bị đổi trong lần này |

Không lỗi nào liên quan học kỳ: các lỗi nằm ở bước phát hành bài nhóm, kết nối AI hoặc thứ tự chạy, và không chạm mã đã sửa.

**Sửa script U12** (theo yêu cầu "Yes, fix them"): `u12.mjs`, `u12b.mjs`, `u12c.mjs` gọi `POST /assignments/{id}/review` (Hoàn tất) và kiểm 200 trước khi phát hành bài nhóm. Chạy lại trên cùng stack (log `.tmp/classterm/e2e-rerun-<bộ>.log`):

| Bộ | Kết quả |
|---|---|
| `u12b` | **36/36 qua** |
| `u12c` | **20/20 qua** |
| `u12` | 58/59. Lỗi còn lại là "U12-02 xem trước chia ngẫu nhiên": lần chạy đầu bị dừng giữa chừng đã chia hết 5 sinh viên của `SE-TEST1` vào nhóm (`ungrouped=0`), nên bản xem trước không còn ai để chia. Ở lần chạy đầu, khi lớp còn 5 sinh viên chưa có nhóm, bước này qua (`sizes=2,2,1`). Đây là giới hạn sẵn có của script khi chạy lại trên lớp cũ, không do lần sửa này |

## Kết luận

- **Build**: thành công.
- **Test**: backend và frontend qua; các test Vitest timeout là do tải máy, chạy lại với thời gian chờ dài hơn thì qua.
- **Trình duyệt**: mọi màn hình có học kỳ đều đã kiểm.
- **E2E**: `u04-u13-u14` 40/40; sau khi sửa script, `u12b` 36/36, `u12c` 20/20, `u12` 58/59 (do dữ liệu của lần chạy trước). `u02`, `u06`, `u06b` lỗi vì lý do không liên quan học kỳ (xem bảng trên).
- **Sẵn sàng cho Operations**: có, sau khi commit và merge theo quy trình hiện có.

Stack `classterm` vẫn đang chạy để xem lại. Để tắt và xóa dữ liệu, chạy lệnh sau từ thư mục `infra/`:

```bash
docker compose -p classterm -f docker-compose.yml -f docker-compose.local.yml -f ../.tmp/classterm/compose.override.yml --env-file ../.tmp/classterm/.env down -v
```
