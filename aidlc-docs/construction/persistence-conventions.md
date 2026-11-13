# Quy định persistence toàn dự án: Spring JDBC

**Có hiệu lực từ 2026-11-06.** Người dùng xác nhận: “Chuyển U05/U09 sang JDBC và sửa AI-DLC quy định toàn dự án dùng JDBC”. Quyết định này thay lựa chọn Spring Data JPA/ORM trong các thiết kế persistence trước đây.

## Công nghệ bắt buộc

- Mọi unit truy cập PostgreSQL bằng **Spring JDBC**: `JdbcTemplate`, `NamedParameterJdbcTemplate` hoặc `JdbcClient`, dùng dependency đã có. Chọn API phù hợp code xung quanh; không thêm thư viện ORM/query builder.
- Không dùng JPA, Hibernate, Spring Data JPA hoặc EntityManager để thực hiện persistence mới. Native SQL qua EntityManager vẫn là truy cập qua JPA, không đáp ứng quy định JDBC này.
- Domain model độc lập ORM; SQL, binding tham số, row mapping và JSONB serialization nằm trong repository của unit. Controller/service không tự chạy SQL; unit khác chỉ gọi public contract. Không tạo interface/Impl hoặc wrapper nếu không có nhu cầu thực tế.
- Giữ schema/Flyway là nguồn định nghĩa bảng và constraint. Không dùng Hibernate create/update schema để thay migration. Không tự đổi API, JSON, public version, ID, event hoặc dữ liệu chỉ để đổi cơ chế truy cập DB.

## Transaction, khóa và version

- Dùng Spring transaction/TransactionTemplate và DataSource chung. Repository JDBC phải tham gia transaction của caller cho các thao tác nhiều bước, audit và outbox. Không tự mở connection/autocommit hoặc REQUIRES_NEW làm tách nghiệp vụ khỏi audit.
- Khóa hàng/advisory cần giữ trên cùng connection tới khi transaction kết thúc. Giữ thứ tự khóa, isolation, timeout và các kiểm tra sau khi chờ khóa theo contract từng unit.
- Version và cập nhật nguyên tử phải viết tường minh: UPDATE với expected version/status, kiểm affected rows, dùng constraint/ON CONFLICT khi cần. Không thay CAS bằng read-then-write không có điều kiện. Public version/storage version giữ quy tắc của từng unit.
- Khi chuyển một unit từ JPA, kiểm cả snapshot, stale writes, JSONB, thứ tự đọc/ghi, rollback, race, idempotency và error mapping. Không coi hết annotation JPA hoặc compile thành công là bằng chứng tương đương.

## SQL và mapping

- Bind mọi giá trị đầu vào bằng tham số. Tên bảng/cột/sort động chỉ lấy từ allowlist nội bộ, không nối trực tiếp đầu vào người dùng vào SQL.
- Giữ scope quyền, bộ lọc, escape tìm kiếm, thứ tự ổn định, offset/limit và zero counts hiện hành. Không mở rộng truy vấn khi scope rỗng.
- JSONB phải lưu JSON object/array đúng contract, không JSON string bọc thêm một lớp. Giữ mapper/module/legacy compatibility, precision của số và timestamp; không phát sinh ghi khi chỉ đọc hoặc sửa object snapshot trong bộ nhớ.
- Phân biệt xung đột version/unique với lỗi DB khác; không nuốt lỗi thành kết quả false hoặc lộ SQL/credential trong lỗi API. Giữ audit thành công cùng transaction và cơ chế ghi thất bại theo contract.

## Phạm vi triển khai và khoảng cách hiện tại

Quy định áp dụng toàn bộ backend. Đợt 2026-11-06 chuyển U05/U09; đợt học liệu 2026-11-09 chuyển thêm material/version adapter của U13. Không tuyên bố toàn dự án đã JDBC chỉ từ việc sửa tài liệu.

| Unit | Hiện trạng và cập nhật đợt U05/U09 | Việc còn lại |
|---|---|---|
| U01/U02/U03 | JDBC | Duy trì quy định |
| U04 subjects | JPA | Cần migration riêng có kiểm chứng khóa/version |
| U05 academics | JDBC | Giữ nguyên, chạy regression |
| U06/U07/U08 | JDBC | Duy trì quy định |
| U09 grading | Đã chuyển evaluation/proposal sang JDBC; visibility giữ JDBC | Duy trì [implementation và kiểm chứng U05/U09](plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md) |
| U10/U11/U12 | JDBC | Duy trì quy định |
| U13 materials | MaterialRepositoryAdapter và material_versions đã JDBC (2026-11-09); MaterialModuleRepositoryAdapter còn JPA | Module persistence cần migration riêng; không xóa hạ tầng ORM chung |
| U14 announcements/notifications | JPA | Cần migration riêng |
| U15 dashboard | Đọc qua contract của owner, không truy cập DB trực tiếp | Giữ ranh giới unit |

Trong giai đoạn chuyển tiếp, dependency JPA và JpaTransactionManager còn phục vụ U04/U13/U14; không xóa hoặc thay transaction manager chung trong thay đổi U05/U09. JDBC phải tham gia đúng transaction trên cùng DataSource. Khi các owner còn lại chuyển xong mới đánh giá loại bỏ hạ tầng ORM khỏi backend.

## Kiểm chứng và tài liệu

Test integration dùng PostgreSQL thật với production migrations/roles; kiểm constraint, locks, CAS, rollback và JSONB. Fixture JDBC được dùng để seed/đối chiếu độc lập. Giữ các test auth/privacy/API/regression và required DB guard; không hạ ngưỡng hoặc bỏ qua test để đạt green.

Tech-stack/logical-components hiện hành tuân theo quyết định này. Báo cáo kiểm thử, audit và kế hoạch có ngày trước đây vẫn là bằng chứng lịch sử; nhận định “đã dùng JPA” trong snapshot cũ không phải chỉ dẫn cho code mới. Khi code một unit chưa chuyển, ghi rõ khoảng cách thay vì sửa snapshot thành hiện trạng giả.
