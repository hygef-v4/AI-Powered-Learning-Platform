# U05 (kèm U04) — Học kỳ của lớp — kế hoạch Functional Design

Kế hoạch dựa trên 2 tài liệu đã duyệt ngày 2026-11-14:

- [Yêu cầu CT-01…09](../../../inception/requirements/2026-11-14-class-term.md) (quyết định D-U04-05 thay D-U04-04).
- [Kế hoạch thực hiện](../../../inception/plans/2026-11-14-class-term-execution-plan.md).

## Bối cảnh từ mã hiện tại

### U05 — lớp

- `CourseClass` là record bất biến: `id, subjectId, code, name, description, status, teacherId, showGradeDistribution, createdAt, updatedAt, version`. `createClass(...)` tạo lớp Nháp; `updateInfo(name, description, now)` chặn lớp lưu trữ (`CLASS_ARCHIVED`) và tăng `version`.
- `CourseClassRepository.insert` map `DuplicateKeyException` sang `CLASS_EXISTS` ("Mã lớp trong môn hoặc ID lớp đã tồn tại"); `update` kiểm `version`.
- Ràng buộc `uq_course_classes_subject_code UNIQUE (subject_id, code)`.
- `AcademicsReadRepository.listForManager/Teacher/Student(query, status, size, offset)` lọc `code`/`name` và sắp `order by c.code, c.id`.
- `AcademicsController`: `CreateInput(subjectId, code, name, description, teacherId)` và `InfoInput(name, description, version)` dùng `IStrictInput` (từ chối trường lạ).
- `ClassRef` (contract cho các module khác) và `SubjectClassSummary` (U04 đọc) không có học kỳ. MCP `list_my_classes` lọc `status`, `query`.
- Luật ghi danh: `activeInOtherClass` (`IN_OTHER_CLASS`) và `conflictingStudents` (`ENROLLMENT_CONFLICT`) xét lớp cùng môn chưa lưu trữ — giữ nguyên theo CT-09.

### Frontend

- `ClassListPage` (Chủ nhiệm môn) và `ClassDashboardPage` (giảng viên, sinh viên) tải toàn bộ lớp rồi lọc phía client; trạng thái lọc giữ trên URL bằng `useUrlState`.
- `ClassLabel` trong `shared/ui/ClassFilter.tsx` hiện mã môn chữ nhỏ ở dòng trên, mã lớp ở dòng dưới; dùng cho ô lọc lớp (Thông báo, Bài tập) và kiểu ô chuyển lớp trên sidebar.
- Mã lớp còn hiện ở những chỗ chỉ đọc: nguồn thông báo, bảng lớp đang giữ khi chặn đổi role, đường dẫn trang lớp, hộp chọn lớp nguồn khi tạo bài tập/quiz từ lớp khác.

## Các bước

- [x] 1. Domain entities U05
  - Thêm `startsOn` (ngày) và `term` (suy ra) cho `CourseClass`.
  - Thêm `ClassTermPolicy`: tính học kỳ, kiểm khoảng ngày, kiểm mẫu `term` cho tham số lọc, thứ tự học kỳ.
  - Ràng buộc dữ liệu: `NOT NULL`, check mẫu, duy nhất `(subject_id, term, code)`, chỉ mục `(subject_id, term)`.
  - `ClassRef` thêm `term`; ghi chú D-U04-05 thay D-U04-04.
- [x] 2. Business rules U05: nhóm quy tắc mới từ BR-U05-61; sửa BR-U05-11 (mã lớp duy nhất theo môn và học kỳ).
  - Tính học kỳ, khoảng ngày, học kỳ chỉ do server tính.
  - Tạo lớp, sửa ngày bắt đầu, tính lại học kỳ, trùng mã.
  - Backfill lớp cũ.
  - Lọc theo `term` ở API và MCP.
  - Luật ghi danh giữ nguyên.
- [x] 3. Business logic model U05
  - Luồng tạo lớp, sửa thông tin lớp có ngày bắt đầu, thứ tự kiểm lỗi.
  - Luồng danh sách lớp có `term`.
  - Thuật toán migration backfill (giờ Việt Nam).
- [x] 4. Frontend components U05
  - `ClassFormDialog` (ô Ngày bắt đầu, xem trước học kỳ), `ClassInfoForm` (sửa ngày, học kỳ chỉ đọc).
  - Bộ lọc học kỳ ở `ClassListPage`, `ClassDashboardPage` (URL `classTerm`, số đếm tab theo kỳ).
  - `ClassLabel`/`ClassFilter`, sidebar, các hộp chọn lớp khác theo Q3.
- [x] 5. Phần sửa U04
  - `SubjectClassSummary` thêm `term`; bảng lớp trong chi tiết môn (Admin) có cột Học kỳ.
  - Ghi chú D-U04-05 thay D-U04-04 trong tài liệu U04.
- [x] 6. Kiểm tra theo Security Baseline và Resiliency Baseline. PBT không áp dụng vì extension này đang tắt.

## Câu hỏi

### Q1 — Giá trị ban đầu của ô Ngày bắt đầu trong form tạo lớp

A) Để trống, Chủ nhiệm môn bắt buộc chọn ngày. Tránh vô tình gán học kỳ hiện tại cho lớp tạo trước cho kỳ sau. (Khuyến nghị)

B) Mặc định là ngày hôm nay, sửa được.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

### Q2 — Khi sửa ngày bắt đầu làm học kỳ thay đổi

A) Lưu thẳng; ngay cạnh ô ngày hiện "Học kỳ: SP27 → SU27" trước khi bấm Lưu, không có hộp xác nhận thêm. (Khuyến nghị)

B) Bấm Lưu thì hiện hộp xác nhận "Học kỳ của lớp sẽ đổi từ SP27 sang SU27".

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

### Q3 — Hiện học kỳ ở những chỗ khác có mã lớp

CT-08 đã chốt: bảng lớp, thẻ Thông tin lớp, ô chuyển lớp trên sidebar, ô lọc lớp. Còn các chỗ: hộp chọn lớp nguồn khi tạo bài tập/quiz từ lớp khác; đường dẫn và tiêu đề trang lớp; nguồn thông báo; bảng lớp đang giữ khi chặn đổi role.

A) Thêm vào hộp chọn lớp nguồn (vì phải chọn đúng lớp) và tiêu đề/đường dẫn trang lớp; các chỗ chỉ đọc còn lại giữ nguyên. (Khuyến nghị)

B) Thêm vào tất cả các chỗ trên.

C) Chỉ đúng phạm vi CT-08, không thêm chỗ nào khác.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

### Q4 — Danh sách học kỳ trong bộ lọc ở trang Lớp học

A) Lấy từ tất cả lớp người dùng đang xem (không phụ thuộc tab trạng thái), để danh sách không đổi khi chuyển tab; tab không có lớp ở kỳ đã chọn thì hiện trạng thái rỗng. (Khuyến nghị)

B) Chỉ lấy học kỳ có trong tab trạng thái đang chọn.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

### Q5 — Ô tìm kiếm mã lớp có tìm theo học kỳ không

A) Không; đã có bộ lọc học kỳ riêng, ô tìm giữ như hiện nay. (Khuyến nghị)

B) Có; gõ "SP27" cũng ra các lớp học kỳ đó.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

### Q6 — Cách hiện học kỳ trong nhãn lớp (sidebar, ô lọc lớp)

Nhãn hiện có hai dòng: mã môn chữ nhỏ ở trên, mã lớp chữ đậm ở dưới.

A) Thêm học kỳ vào dòng nhỏ phía trên: `PRJ301 · SP27`, dòng dưới vẫn là `SE1801`. Giữ dòng chính ngắn cho sidebar hẹp. (Khuyến nghị)

B) Thêm sau mã lớp ở dòng chính: `SE1801 · SP27`.

X) Khác (mô tả sau [Answer]:)

[Answer]: A (theo khuyến nghị)

## Kết quả

Tài liệu đã cập nhật (2026-11-14):

- U05: [domain-entities.md](../../u05-class-enrollment/functional-design/domain-entities.md) (mục 2.1 `ClassTermPolicy`, 2.2 ràng buộc dữ liệu, `ClassRef.term`), [business-rules.md](../../u05-class-enrollment/functional-design/business-rules.md) (sửa BR-U05-11, 13; thêm BR-U05-61…72), [business-logic-model.md](../../u05-class-enrollment/functional-design/business-logic-model.md) (F17–F20), [frontend-components.md](../../u05-class-enrollment/functional-design/frontend-components.md) (mục "Học kỳ của lớp").
- U04: domain-entities, business-rules, frontend-components (mục "Học kỳ của lớp", `SubjectClassSummary.term`, cột Học kỳ ở `SubjectClassesTable`).
- Phần nhỏ của U07: `CopySource` thêm `term` lấy từ `ClassRef.term` (hộp chọn lớp nguồn, FD Q3). Quiz dùng danh sách lớp đã tải, không đổi backend U10.

## Tuân thủ extension

| Rule | Kết quả | Ghi chú |
|---|---|---|
| SECURITY-03 Logging | Tuân thủ | Không thêm log mới; audit giữ `CLASS_CREATED`/`CLASS_UPDATED` (BR-U05-72). Học kỳ không phải dữ liệu nhạy cảm. |
| SECURITY-04 HTTP headers | N/A | Không đổi cấu hình HTTP hay header. |
| SECURITY-05 Input validation | Tuân thủ | `startsOn` kiểm khoảng ở domain; `term` lọc kiểm mẫu chính xác; `term` trong body bị strict-input từ chối; SQL dùng tham số (BR-U05-61, 62, 65). |
| SECURITY-08 Access control | Tuân thủ | Tạo/sửa chỉ Chủ nhiệm môn của môn; lọc `term` chỉ thu hẹp trong phạm vi quyền sẵn có (BR-U05-63, 65). |
| SECURITY-09 Hardening | Tuân thủ | Lỗi trùng khóa đổi thành `CLASS_EXISTS`, không lộ lỗi database (BR-U05-64). |
| SECURITY-12 Authentication | N/A | Không đổi xác thực hay thông tin đăng nhập. |
| SECURITY-15 Exception handling | Tuân thủ | Lỗi miền có mã rõ (`INVALID_CLASS_START_DATE`, `INVALID_CLASS_TERM`, `CLASS_EXISTS`); thao tác sửa rollback toàn bộ khi lỗi (F18). |
| RESILIENCY-04 Deployment/rollback | Tuân thủ | Migration mới, không sửa migration cũ; rủi ro rollback (cần backup khi đã có lớp trùng mã khác kỳ) ghi trong execution plan. |
| RESILIENCY-06 Health checks | N/A | Không đổi health check. |
| RESILIENCY-10 Dependency isolation | N/A | Không thêm phụ thuộc ngoài; contract `ClassRef`/`SubjectClassSummary` chỉ thêm trường. |
| PBT | N/A | Extension tắt. |
