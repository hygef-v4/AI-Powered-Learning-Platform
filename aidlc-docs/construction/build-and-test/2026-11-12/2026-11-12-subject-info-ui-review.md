# Báo cáo: màn Thông tin môn (Chủ nhiệm môn) so với Thông tin lớp (Giảng viên)

Ngày: 2026-11-12. Phạm vi: `/manager/subjects/{id}` (`MySubjectPage`, `frontend/src/features/academics/pages/ManagerDashboardPage.tsx:45`) so với tab `info` của `ReadOnlyClassPage` (`frontend/src/features/academics/pages/ReadOnlyClassPage.tsx:80`).

## A. Lỗi dữ liệu (cần sửa)

| # | Vấn đề | Nguyên nhân | Đề xuất |
|---|--------|-------------|---------|
| A1 | **Đã sửa 2026-11-12.** Mô tả môn luôn hiện "Chưa có mô tả." kể cả khi Quản trị đã nhập | `GET /api/manager/subjects/{id}` trả `SubjectSummary(id, code, name, status, classCounts)` (`backend/.../academics/dto/AcademicsDtos.java:20`) — không có `description`. Contract `SubjectRef` (`backend/.../subjects/contract/SubjectRef.java`) cũng không mang `description`, dù model `Subject` có | Thêm `description` vào `SubjectRef` và `SubjectSummary`, map trong `AcademicsService.mapSubjectSummary` (`:538`); cập nhật test hợp đồng |
| A2 | **Đã sửa 2026-11-13.** "Số lớp" chỉ đếm lớp `OPEN` từ danh sách lớp tải riêng (`countOpenClasses`), bỏ qua `classCounts` backend đã trả | Trang tải thêm toàn bộ lớp của mọi môn (`allClasses("manager")` trong `ClassRoutes.tsx:169`) chỉ để đếm | Dùng `subject.classCounts`; hiện rõ "Đang mở / Lưu trữ"; bỏ tải toàn bộ lớp ở màn này |
| A3 | **Đã sửa 2026-11-13.** Không hiển thị trạng thái môn (Hoạt động / Lưu trữ) dù API có `status` | UI không dùng trường này | Thêm ô Trạng thái như màn lớp |

## B. Chưa đồng bộ giao diện với màn Giảng viên

| # | Màn Giảng viên (Thông tin lớp) | Màn Chủ nhiệm môn (Thông tin môn) | Đề xuất |
|---|---|---|---|
| B1 | **Đã sửa 2026-11-13.** Dạng chỉ xem `<dl>`: nhãn nhỏ màu muted + giá trị đậm, lưới 4 cột | Dùng `LockedInput` (ô nhập bị khóa) — trông như form có thể sửa nhưng không sửa được | Chuyển sang cùng kiểu `<dl>` 4 cột: Mã môn · Tên môn · Số lớp · Trạng thái |
| B2 | **Đã sửa 2026-11-13.** Mô tả là đoạn văn thường, ẩn nếu rỗng | Ô mô tả cao `min-h-28` có viền, hiện placeholder khi rỗng | Hiện đoạn văn như màn lớp; ẩn hoặc 1 dòng muted khi rỗng |
| B3 | **Đã sửa 2026-11-13.** Có thẻ "Tổng quan bài tập" (Bài đang mở, Bài nộp chờ chấm, Đã chấm chưa công bố, Hạn nộp sắp tới) qua slot `teacherClassTabs` | Chỉ có 1 thẻ, không có tổng quan | Thêm "Tổng quan môn" (xem C) qua một slot riêng cho trang môn, giống cơ chế `SlotOutlet` |
| B4 | **Đã sửa 2026-11-13.** Tiêu đề trang = tên mục sidebar ("Thông tin lớp") | "Thông tin môn" nhưng thẻ lại tên "Thông tin môn học" | Thống nhất: tiêu đề "Thông tin môn", thẻ "Thông tin môn" (như lớp dùng cùng một tên) |
| B5 | **Đã sửa 2026-11-13.** Bố cục có `data-teacher-overview`, khoảng cách `gap-4` giữa các thẻ | Một thẻ đơn, nửa dưới màn trống | Bố cục cột dọc nhiều thẻ như màn lớp |

## C. Thông tin nên bổ sung (theo vai trò Chủ nhiệm môn)

1. **Lớp của môn**: danh sách ngắn các lớp đang mở (mã lớp, giảng viên, số sinh viên) + liên kết "Tất cả lớp" tới `/manager/classes?subjectId=`. Dữ liệu đã có (`CourseClassView` có `teacher`, `activeStudentCount`).
2. **Tổng số sinh viên / giảng viên** của môn: tổng hợp từ các lớp đang mở.
3. **Tổng quan nội dung môn**: số học liệu, số bài tập mẫu, số câu hỏi trong Ngân hàng câu hỏi (kèm số đề xuất câu hỏi chờ duyệt) — liên kết tới các mục sidebar tương ứng. Cần kiểm tra/bổ sung API đếm theo `subjectId`.
4. **Lớp chưa phân công giảng viên**: cảnh báo nếu có lớp `teacher == null`.

## D. Ghi chú kỹ thuật

- Comment JSDoc của `MySubjectPage` ("Không có ô số liệu khác") cần cập nhật khi đổi thiết kế.
- Test liên quan: `frontend/tests/features/academics/components/integration.test.tsx` render `MySubjectPage` — cần cập nhật theo bố cục mới.

## Thứ tự ưu tiên đề xuất

1. A1 (lỗi dữ liệu thật, người dùng thấy ngay). 2. A2, A3, B1, B2, B4 (đồng bộ thẻ thông tin, chỉ frontend). 3. B3 + C (tổng quan môn, cần thêm API/slot).

## E. Kết quả sửa 2026-11-13 (A2, A3, B1–B5)

- `MySubjectPage` nhận `ManagedSubject`; thẻ "Thông tin môn" dạng `<dl>` 4 cột (Mã môn, Tên môn học, Số lớp từ `classCounts`, Trạng thái), mô tả là đoạn văn và ẩn khi rỗng.
- `ManagerRoute` chỉ tải toàn bộ lớp khi mở mục Lớp học.
- B3: thêm `SubjectClassesOverview` (thẻ "Tổng quan lớp": số lớp đang mở, tổng sinh viên, lớp chưa có giảng viên, danh sách lớp đang mở), tải `GET /manager/classes?subjectId=&status=OPEN`. Render trực tiếp trong academics, chưa tạo slot vì chưa có unit nào cần đăng ký. Như vậy đã phủ C1, C2, C4; còn C3 (học liệu, bài tập, câu hỏi theo môn) cần API đếm.
- Kiểm thử: vitest `tests/features/academics` 69/69, `tsc --noEmit` và eslint sạch; xem trên app local với tài khoản Chủ nhiệm môn test (SWT301).
