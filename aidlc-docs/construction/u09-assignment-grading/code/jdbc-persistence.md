# U09 persistence — JDBC, 2026-11-06

U09 truy cập PostgreSQL hoàn toàn qua Spring JDBC theo [quy định toàn dự án](../../persistence-conventions.md). U05 đã JDBC nên không cần chuyển production code.

| Thành phần | Hiện tại |
|---|---|
| `EvaluationRepository` | NamedParameterJdbcTemplate; JSONB và indexed columns cập nhật cùng statement; insert storage/public version 0, update có CAS/version và RETURNING |
| `GradingProposalRepository` | NamedParameterJdbcTemplate; lookup ID/requestRef, danh sách version_no DESC; khóa row/cập nhật storage version tường minh |
| `EvaluationRow`, `GradingProposalRow` | Mapping JSON text thuần, không annotation/entity/ORM; trả aggregate snapshot độc lập |
| `GradeDistributionVisibilityRepository` | JdbcTemplate hiện hữu; giữ mặc định off/version 0 và INSERT ON CONFLICT/CAS theo bài–lớp |

Hai Spring Data interface và hai entity JPA cũ đã được loại bỏ/thay thế trong U09. Service/interface/HTTP/JSON/schema/migrations giữ nguyên. JSON mapper tiếp tục hỗ trợ Java time và bỏ qua field chưa biết; storage JSONB vẫn là object. DB version của Evaluation ghi đè version trong JSON khi đọc, như trước.

`lockById` dùng SELECT FOR UPDATE và `lockIdentity` giữ nguyên key/hash của advisory transaction lock để tuần tự hóa cả trường hợp chưa có evaluation. Hai method yêu cầu caller transaction. `save` tham gia transaction Spring hiện có, cập nhật đồng bộ và kiểm xung đột; rollback của caller khôi phục cả JSONB/version. Mutation snapshot trong bộ nhớ hoặc polling không tự ghi DB.

Gọi `save` tường minh trên row đã có luôn tăng storage version một lần, kể cả payload không đổi; khác dirty checking JPA có thể bỏ qua no-op trong cùng persistence context. Không thêm cache để mô phỏng ORM. `explicitNoOpSavesAdvanceVersionsButReadsDoNot` bảo vệ quy tắc mới này; version proposal là nội bộ. Review caller xác nhận command chấm điểm đổi state/timestamp, duplicate submission trả về trước save và proposal saves được tuần tự hóa bằng khóa evaluation cha; polling không save. Đây không phải bằng chứng tương đương từng chi tiết nội bộ ORM.

Ứng dụng vẫn cần JpaTransactionManager/dependency JPA cho U04/U13/U14 chưa chuyển; đó không phải JPA persistence trong U09. Test dùng cả DataSourceTransactionManager và JpaTransactionManager với cùng DataSource để bảo vệ rollback trong giai đoạn chuyển tiếp.

Không có migration dữ liệu/schema mới. Không tự chuyển unit khác, đổi frontend, restart/migrate live DB hoặc commit/push. [Kế hoạch](../../plans/code-generation-plan/2026-11-06-u05-u09-jdbc.md) và [bằng chứng/giới hạn](../../build-and-test/2026-11-06-u05-u09-jdbc-results.md) ghi kết quả kiểm chứng thực tế; các snapshot 2026-10-30 và báo cáo JPA cũ giữ giá trị lịch sử.
