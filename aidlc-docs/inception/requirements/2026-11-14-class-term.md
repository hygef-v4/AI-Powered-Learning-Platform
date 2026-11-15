# Học kỳ của lớp — yêu cầu

Mở rộng U05 (lớp, ghi danh), kèm phần hiển thị ở U04 (môn học) và các nhãn chọn lớp dùng chung. Quyết định: [câu hỏi xác nhận](2026-11-14-class-term-questions.md), người dùng trả lời trong chat ngày 2026-11-14 (Q4–Q8 theo khuyến nghị).

**Quyết định D-U04-05 (2026-11-14):** lớp có lại học kỳ, nhưng học kỳ **do hệ thống tính** từ ngày bắt đầu của lớp, không nhập tay. Quyết định này thay thế D-U04-04 (2026-11-07, bỏ học kỳ khỏi lớp). Migration `V20261107_2200__remove_class_term.sql` giữ nguyên; thêm migration mới. Giá trị học kỳ cũ đã bị xóa theo D-U04-04 không khôi phục.

## Phân tích yêu cầu

- **Yêu cầu**: "tôi muốn thêm trường học kỳ cho lớp, mục đích để lọc lớp theo kỳ. Tôi không muốn thêm các chức năng về quản lý học kỳ".
- **Loại**: Enhancement. Thêm ngày bắt đầu và học kỳ suy ra cho lớp, bộ lọc theo học kỳ.
- **Phạm vi**:
  - Backend: `academics` gồm bảng `course_classes`, model `CourseClass`, repository, service, controller, DTO `ClassView`, contract `ClassRef`, MCP tool `list_my_classes`; `subjects` gồm `SubjectClassSummary`.
  - Frontend: `features/academics` (form tạo/sửa lớp, danh sách lớp, trang lớp), `features/subjects` (bảng lớp của môn), `shared/ui/ClassFilter`, ô chuyển lớp trên sidebar.
  - Seed e2e.
- **Độ phức tạp**: vừa. Có một quy tắc nghiệp vụ mới (tính học kỳ), đổi ràng buộc duy nhất và một migration có cập nhật dữ liệu.
- **Độ sâu yêu cầu**: Standard.
- **Không đổi**: không có danh mục, màn hình hay API quản lý học kỳ. Quyền, trạng thái lớp, luật ghi danh, bài tập, quiz, điểm và nhóm giữ nguyên.

## Thuật ngữ

- **Ngày bắt đầu**: ngày lớp bắt đầu học (chỉ ngày, không giờ).
- **Học kỳ (term)**: mã do hệ thống tính từ ngày bắt đầu, ví dụ `SP27`, `SP27-BL5`.
- **Lớp cũ**: lớp đã có trước khi tính năng được triển khai.

## Yêu cầu chức năng

### CT-01 — Ngày bắt đầu bắt buộc khi tạo lớp (Q4)

- Form tạo lớp có ô **Ngày bắt đầu**, bắt buộc. API tạo lớp nhận trường `startsOn` (ISO `YYYY-MM-DD`), bắt buộc.
- Ngày bắt đầu phải nằm trong khoảng 2000-01-01 đến 2099-12-31 để hai chữ số năm không bị nhập nhằng. Sai thì báo lỗi `INVALID_CLASS_START_DATE`.
- Ngày bắt đầu được phép ở quá khứ hoặc tương lai (tạo lớp trước cho kỳ sau).

### CT-02 — Quy tắc tính học kỳ (Q1, Q2)

- Học kỳ = mã kỳ theo tháng của ngày bắt đầu + 2 chữ số cuối của năm của ngày bắt đầu:
  - Tháng 1–4: `SP`; tháng 5–8: `SU`; tháng 9–12: `FA`.
- Nếu tháng bắt đầu là 4, 8 hoặc 12 thì thêm hậu tố `-BL5` (lớp block 5).
- Mẫu hợp lệ: `^(SP|SU|FA)[0-9]{2}(-BL5)?$`, tối đa 8 ký tự.
- Ví dụ: 2027-01-10 → `SP27`; 2027-04-03 → `SP27-BL5`; 2027-05-15 → `SU27`; 2027-08-20 → `SU27-BL5`; 2026-09-07 → `FA26`; 2026-12-01 → `FA26-BL5`.
- `SP27` và `SP27-BL5` là hai học kỳ khác nhau khi lọc và khi xét trùng mã lớp.
- Học kỳ được tính trong domain và lưu vào cột `term`. API không nhận `term` từ client; request có trường `term` bị từ chối như trường lạ theo strict-input hiện có.

### CT-03 — Sửa ngày bắt đầu (Q5)

- Ở thẻ Thông tin lớp, Chủ nhiệm môn sửa được Ngày bắt đầu khi lớp chưa lưu trữ (Nháp hoặc Đang mở). API sửa thông tin lớp nhận `startsOn` (bắt buộc, gửi giá trị hiện tại nếu không đổi).
- Học kỳ được tính lại theo ngày mới trong cùng thao tác. Kiểm tra `version` như các thao tác sửa hiện có.
- Lớp lưu trữ chỉ xem, không sửa được ngày bắt đầu (`CLASS_ARCHIVED` như hiện nay).
- Nếu học kỳ mới làm trùng mã lớp với lớp khác cùng môn ở học kỳ đó thì báo lỗi `CLASS_EXISTS` (CT-04) và không lưu.

### CT-04 — Mã lớp duy nhất theo môn và học kỳ (Q3)

- Ràng buộc duy nhất đổi từ `(subject_id, code)` thành `(subject_id, term, code)`. Cùng một mã lớp được dùng lại ở các học kỳ khác nhau của cùng môn.
- Trùng mã trong cùng môn và cùng học kỳ: lỗi `CLASS_EXISTS`, thông báo "Mã lớp đã tồn tại trong môn ở học kỳ này".

### CT-05 — Lớp cũ (Q6)

- Migration mới gán ngày bắt đầu = ngày của `created_at` theo giờ Việt Nam (`Asia/Ho_Chi_Minh`) và tính học kỳ theo CT-02 cho mọi lớp cũ.
- Sau migration, `starts_on` và `term` đều không được trống. Không có lớp "chưa có học kỳ".
- Migration không đổi id, version, trạng thái, ghi danh hay dữ liệu học tập của lớp. Dữ liệu hiện có đã duy nhất theo `(subject_id, code)` nên cũng duy nhất theo `(subject_id, term, code)`.
- Chủ nhiệm môn sửa ngày bắt đầu của lớp cũ theo CT-03 nếu ngày tạo không đúng ngày bắt đầu thật.

### CT-06 — Lọc lớp theo học kỳ

- Bộ lọc học kỳ đặt cạnh tab trạng thái ở:
  - Trang Lớp học của Chủ nhiệm môn (danh sách lớp của môn đang chọn).
  - Trang Lớp học của giảng viên và của sinh viên.
- Lựa chọn: "Tất cả học kỳ" (mặc định), sau đó các học kỳ có trong danh sách lớp đang tải, mới nhất lên trước (theo năm, rồi FA > SU > SP, bản `-BL5` đứng trước kỳ gốc cùng mùa vì bắt đầu muộn hơn).
- Lựa chọn được giữ trên URL (tham số `classTerm`), giống bộ lọc trạng thái hiện có. Giá trị trên URL không có trong danh sách thì về "Tất cả học kỳ".
- Bộ lọc học kỳ kết hợp với tab trạng thái và ô tìm mã lớp. Số đếm trên tab trạng thái tính theo học kỳ đang chọn.
- Đổi học kỳ đưa phân trang về trang đầu.

### CT-07 — API và contract

- Ba API danh sách lớp `/me/classes`, `/teaching/classes`, `/manager/classes` nhận thêm tham số tùy chọn `term`. Giá trị sai mẫu CT-02 thì lỗi `INVALID_CLASS_TERM`.
- `ClassView` thêm `startsOn` và `term`. `ClassRef` và `SubjectClassSummary` thêm `term`.
- MCP tool `list_my_classes` trả thêm `term` và nhận tham số tùy chọn `term`.
- Không thêm API nào khác.

### CT-08 — Hiển thị học kỳ (Q8)

- Học kỳ hiện ở:
  - Các bảng danh sách lớp (Chủ nhiệm môn, giảng viên, sinh viên) và bảng lớp trong chi tiết môn của Admin: cột hoặc nhãn "Học kỳ".
  - Thẻ Thông tin lớp: Ngày bắt đầu (sửa được theo CT-03) và Học kỳ (chỉ đọc, cập nhật theo ngày).
  - Mọi nhãn chọn lớp: ô chuyển lớp trên sidebar và ô lọc lớp `ClassFilter` (Thông báo, Bài tập), dạng mã lớp kèm học kỳ, ví dụ `SE1801 · SP27`.
- Form tạo lớp hiện trước học kỳ sẽ được gán ngay khi chọn ngày bắt đầu.

### CT-09 — Luật ghi danh không đổi (Q7)

- Giữ nguyên `IN_OTHER_CLASS` (khi ghi danh) và `ENROLLMENT_CONFLICT` (khi mở lớp): sinh viên không được đang ghi danh ở hai lớp cùng môn chưa lưu trữ, bất kể học kỳ.
- Lớp kỳ trước phải được lưu trữ thì sinh viên mới vào lớp cùng môn ở kỳ sau.

## Giả định

- **G1**: Hai chữ số năm lấy theo năm dương lịch của ngày bắt đầu (Spring 2027 → `SP27`, Fall 2026 → `FA26`).
- **G2**: Học kỳ không đưa vào prompt AI, thông báo hay email.
- **G3**: Thứ tự danh sách lớp giữ theo mã lớp; lớp trùng mã thì học kỳ mới hơn đứng trước.
- **G4**: Không có bộ lọc "học kỳ hiện tại" tự động; người dùng tự chọn học kỳ.

## Yêu cầu phi chức năng

- **NFR-CT-01 (Bảo mật, SECURITY-05)**: backend kiểm `startsOn` và tham số `term`; học kỳ chỉ do server tính. Frontend kiểm trước để báo lỗi sớm.
- **NFR-CT-02 (Bảo mật, SECURITY-08)**: học kỳ chỉ trả qua các API lớp hiện có, theo đúng phạm vi quyền hiện có; lọc theo `term` không mở rộng phạm vi lớp được xem.
- **NFR-CT-03 (Dữ liệu)**: migration mới thêm `starts_on DATE` và `term VARCHAR(8)`, backfill theo CT-05, đặt `NOT NULL`, thêm ràng buộc kiểm mẫu `term`, đổi ràng buộc duy nhất sang `(subject_id, term, code)`. Không sửa migration cũ.
- **NFR-CT-04 (Hiệu năng)**: chỉ mục hỗ trợ lọc `(subject_id, term)`; truy vấn danh sách vẫn một truy vấn có phân trang.
- **NFR-CT-05 (Tương thích)**: `ClassView`, `ClassRef`, `SubjectClassSummary` chỉ thêm trường. API tạo và sửa lớp bắt buộc thêm `startsOn`; frontend, seed và script e2e cập nhật cùng đợt.
- **NFR-CT-06 (Resiliency)**: sửa ngày bắt đầu dùng khóa và kiểm `version` như sửa thông tin lớp hiện có; trùng mã do tính lại học kỳ trả `CLASS_EXISTS`, không để lỗi database lọt ra.
- **NFR-CT-07 (Kiểm thử)**: unit test cho quy tắc tính học kỳ (đủ 12 tháng, biên năm), tạo/sửa lớp, trùng mã theo học kỳ; test PostgreSQL cho migration (backfill theo giờ Việt Nam, ràng buộc duy nhất mới); test API cho `startsOn`, `term` và từ chối `term` trong body; Vitest cho form, bộ lọc và nhãn lớp. Toàn bộ `mvn verify` và Vitest phải qua.
