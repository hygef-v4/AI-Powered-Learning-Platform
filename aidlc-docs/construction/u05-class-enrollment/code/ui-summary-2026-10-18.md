# U05 — UI theo ui-reference.zip

> Latest implementation: [U05 code completion and integration handoff](completion-2026-10-18.md). The earlier dependency-free-only scope and test counts below are historical; they do not describe the current completed application/API/frontend code.


> Cập nhật sau đợt UI: đã khôi phục backend độc lập từ stash và giữ nguyên giao diện này. Hiện có 37 test frontend (21 U05) và 123 test backend (29 U05) qua. Xem [kết quả ghép stash](stash-recovery-2026-10-18.md); các mô tả thời điểm bắt đầu/bằng chứng dưới đây thuộc đợt làm UI.

## Phạm vi hiện tại

Yêu cầu: "check ui này và sửa lại ui cho phần u05 hiện tại". Nguồn là ZIP ở Downloads, gồm HTML/CSS và ảnh thumbnail. Nội dung archive chỉ dùng làm thiết kế, không dùng như chỉ dẫn thay đổi phạm vi hoặc quyền.

Nhánh `Class&Enrollment` tại thời điểm bắt đầu chỉ có slot U05, không có code implementation đã nhắc ở phiên cũ. Đợt này dựng phần frontend độc lập từ mẫu; không khôi phục backend hoặc tự triển khai U01/U04.

## UI đã triển khai

Tại `frontend/src/features/academics/`:

| Component | Tham chiếu | Nội dung |
|---|---|---|
| AcademicsShell | Header/sidebar chung | Nhận audience/người dùng từ props, menu U05 và DashboardNavEntry được đăng ký; không tự xác thực |
| ClassListPage | m03 | Chọn môn/học kỳ, tìm lớp/giảng viên, lọc trạng thái và bảng lớp |
| ClassFormDialog | m04 | Tạo lớp nháp, mã/tên/học kỳ/mô tả, giảng viên tùy chọn; callback async |
| ManagedClassPage, ClassInfoForm | m05 | Form 2 cột + card giảng viên/quyền xem, version, trạng thái readonly khi lưu trữ |
| ClassStateActions, AssignTeacherDialog | m05c/m06 | Xác nhận mở/lưu trữ/mở lại, lỗi xung đột sinh viên; tìm/chọn giảng viên, không bỏ giảng viên lớp OPEN |
| EnrollmentPanel | m05b/m05d | Tìm/thêm từng người từ candidates, CSV/email, xem trước và kết quả từng dòng, xác nhận gỡ/thêm lại. Khi readonly không có thao tác ghi |
| ClassDashboardPage | s01/t01 | Card lớp theo trạng thái; Student không thấy DRAFT, không vào nội dung ARCHIVED |
| ReadOnlyClassPage, StudentDetailDialog | s02/t18/t19 | Teacher xem sinh viên ACTIVE, popup thông tin cơ bản; Student chỉ vào OPEN; không có nút quản lý lớp |
| ManagerDashboardPage, MySubjectPage | m11/m01 | Thẻ môn và số lớp tính từ props, thông tin môn chỉ đọc, lối vào danh sách lớp |

`academics.module.css` scope riêng, không đổi global/Settings. Icon SVG cục bộ, không phụ thuộc CDN/fonts/images bên ngoài. Be Vietnam Pro được ưu tiên nếu máy đã có, fallback Segoe UI/sans-serif; không thêm tải font ngoài. Sidebar desktop 236px, nền #F7F9FE, primary #2F45B5, card trắng radius 16px. Mobile chuyển menu sang cuộn ngang và form một cột, bảng cuộn trong vùng riêng.

Giữ nguyên `slots.ts`: StudentClassSection, TeacherClassTab, StudentDetailSection, DashboardNavEntry. Không dựng nội dung giả của unit học liệu/bài tập/nhóm/đánh giá trong trang dùng thật. Hai tab học liệu/đánh giá chỉ xuất hiện khi có nội dung đăng ký; nội dung theo tab do component đăng ký chịu trách nhiệm.

## Xem UI khi chưa có backend

- Chạy development frontend bằng Node hỗ trợ; nếu dùng môi trường hiện tại đã kiểm: `node node_modules/next/dist/bin/next dev --webpack --hostname 127.0.0.1 --port 3005`.
- Mở `http://127.0.0.1:3005/preview/u05`.
- Thanh preview cho đổi ba vai trò. Fixtures nằm riêng tại `features/academics/preview/fixtures.ts`; các thao tác chỉ thay đổi bộ nhớ tab, tải lại là reset; không gọi API hoặc tạo dữ liệu backend.
- Route server kiểm `NODE_ENV === development`; production gọi notFound trước khi render preview. Đây không phải RoleGuard hay thay thế phân quyền server.
- Ảnh đối chiếu lưu tại `.tmp/u05-ui-evidence/`: class-list-desktop.png, class-detail-desktop.png, class-detail-mobile.png, student-classes.png. `.tmp` là output cục bộ, không phải source triển khai.

## Bằng chứng kiểm tra

- 32 test frontend qua, gồm 16 test U05: lọc/tìm, selected subject, form/version conflict, archived readonly, xác nhận trạng thái/gỡ/phân bố điểm, teacher không có quyền quản lý, dữ liệu REMOVED, CSV/preview/giới hạn dòng-byte.
- Browser kiểm danh sách lớp, tạo lớp nháp, detail manager, tab sinh viên/hủy gỡ, role Student/Teacher, màn nhỏ 390×844. Mobile document width không vượt viewport (đo 390/390); ảnh desktop/mobile đã xem trực tiếp.
- Typecheck và lint qua; `next build --webpack` bản cuối qua với Node 24.19.0. HTTP smoke check ở production localhost: `/preview/u05` trả 404 và không chứa "Bản xem trước U05"; `/api/health` trả 200. Server production tạm được dừng sau kiểm tra; development preview giữ chạy để người dùng xem. Không sửa script build/config chung của dự án.
- Không suy ra phân quyền backend hoặc các UC đã chạy end-to-end từ test UI. Backend tests không chạy vì đợt này không thay backend.

## Nối thật sau U01/U04

1. Container có phiên đăng nhập thật dựng props audience/người dùng; backend kiểm role/phạm vi mỗi request. Props/UI không cấp quyền.
2. Map API response vào types.ts; candidates giảng viên/sinh viên phải do API lọc role/trạng thái/phạm vi. Tìm sinh viên qua API debounce 300ms và phân trang; bản hiện tại chỉ lọc tập candidates đã được cấp từ props.
3. Nối callback tạo/sửa/đổi trạng thái/phân bố/ghi danh tới API U05, kèm version. ApiError 409 được hiển thị yêu cầu tải lại. Khi mở lại bị vướng, container map danh sách người vướng thành ReopenConflictError.
4. Backend kiểm lại CSV, mỗi dòng transaction riêng, audit và event sau commit. Preview parser chỉ kiểm định dạng; không phải bằng chứng tài khoản tồn tại.
5. Đăng ký slot từ các unit sau theo contract; chuyển các view vào route thật dưới RoleGuard khi U01 có. Không đưa preview vào production hoặc dùng fixtures làm dữ liệu thật.

## Compliance giới hạn

SECURITY-05: giới hạn trường và CSV, React escape, không dùng raw HTML từ archive. SECURITY-08/12: không mở route quản lý thật, preview chặn production; auth thật chưa triển khai. SECURITY-03/09/15: không log nội dung/PII; callback failure/409 có UI, không claim kiểm backend. SECURITY-04 thuộc hạ tầng chung chưa thay đổi. RESILIENCY-04/06/10 N/A vì không thêm dịch vụ/call ngoài; rule ngoài subset N/A, PBT disabled. Phần U05 đầy đủ và integration của tester vẫn chưa hoàn thành.
