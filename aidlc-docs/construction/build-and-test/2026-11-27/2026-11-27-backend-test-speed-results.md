# Tăng tốc bộ test backend — kết quả đo 27/11

**Nguồn:** `staging@19e7bc00` (trước) và nhánh `perf/backend-test-speed` dựng trên commit đó, chưa commit (sau). **Máy:** Windows 11, 12 nhân, RAM 15,8 GB; Docker Desktop 12 CPU, 8,3 GB; Maven 3.9.9, JDK 21.0.11 (CI dùng JDK 17). Stack `systest` vẫn bật nhưng không có tải. Không đổi code production.

## Thay đổi

| Phần | Nội dung |
|---|---|
| Postgres dùng chung | `backend/src/test/java/edu/aiplatform/shared/testsupport/SharedPostgres.java`: mỗi JVM chỉ bật một container `postgres:16-alpine`. Mỗi lớp test nhận một database mới và trống (`createDatabase()`), hoặc một database đã được cấp quyền theo `infra/postgres/init/01-roles.sh` (`createDatabaseWithRoles()`). Role `migrator`, `app`, `backup_replicator` là đối tượng của cả cluster nên được tạo một lần theo chính script đó. Database chỉ được tạo khi lớp test dùng lần đầu, nên lớp bị tắt vì thiếu Docker vẫn được bỏ qua như trước. |
| 30 lớp test tích hợp | Bỏ `@Container static PostgreSQLContainer`, chuyển sang `SharedPostgres`. Bỏ các câu `create role app` và file `requirements/platform-roles.sql`; `grading/prerequisites.sql` không tạo role nữa. Flyway, dữ liệu và cách dọn dữ liệu trong từng lớp giữ nguyên. |
| Surefire | `forkCount=3` (đổi bằng `-Dtest.forkCount=N`), `reuseForks=true`, nên mỗi fork có container riêng. Lượt `serial-tests` chạy các lớp có tag `serial` trong một JVM, sau lượt song song. |
| Tag | `serial`: `RequirementsCoverageIntegrationTest` (có hai test đo p95/fanout), `DriveHealthIndicatorTest` (chờ probe 2 s). `perf`: hai test đo của `RequirementsCoverageIntegrationTest`. `slow`: sáu test trích xuất thật trong `MaterialTextExtractorTest` (mỗi test bật JVM con, có PDF 50 MB). |
| Profile `quick` | `mvn verify -Pquick` bỏ tag `slow` và `perf`. CI vẫn chạy `mvn -B verify` đủ test. |
| Ổn định khi chạy song song | RabbitMQ chờ khởi động 3 phút thay vì 60 s (4 lớp). `CrossUnitFlowIntegrationTest`, `DriveStorageIntegrationTest` tắt bản in MockMvc, vì bản in đọc header trong khi luồng async tải file còn ghi. `JobDispatcherTest`: timeout mặc định của handler giả tăng từ 200 ms lên 1 s; test budget theo message ngủ 1,2 s để vẫn vượt mức mặc định. |

## Số đo

Mỗi lượt là `mvn -o -B verify`, tính cả thời gian biên dịch. Các lượt sau thêm `clean`.

| Lượt | Thời gian | Lớp | Test | Lỗi | Bỏ qua |
|---|---|---|---|---|---|
| Trước: `staging@19e7bc00`, chạy nối tiếp một JVM | **29:12** | 296 | 2.750 | 1 (`localOwnerReadLatency…`, U14_ANNOUNCEMENTS p95 337 ms > 300 ms) | 1 |
| Sau: 3 fork + lượt serial (mặc định) | **15:02** | 296 | 2.750 | 0 | 1 |
| Sau: 4 fork + lượt serial | 14:17 | 296 | 2.750 | 0 | 1 |
| Sau: `-Pquick`, 3 fork | 13:06 | 296 | 2.742 (bỏ 8 test `slow`/`perf`) | 0 | 1 |

Tổng thời gian giảm khoảng 49% (29:12 → 15:02). Profile `quick` chỉ bớt thêm khoảng 2 phút, vì phần lớn thời gian nằm ở các lớp tích hợp Spring không gắn tag. Test bỏ qua ở cả hai bên là `GradingAiEvaluationTest`, cần khóa API. Lượt 3 fork đạt `scripts/assert-required-backend-tests.ps1`: 7 suite bắt buộc và 57 ca truy vết đều qua.

Số đo hiệu năng ở lượt serial (ngưỡng trong ngoặc): U04_SCOPE 8 ms (20), U13_LIST 223 ms (300), U14_ANNOUNCEMENTS 118 ms (300), fanout 200 thông báo 2.460 ms (5.000). Ở baseline, U14_ANNOUNCEMENTS lỗi (337 ms) dù chạy nối tiếp. Test p95 nhạy với tải của máy, nên có thể còn lỗi lại khi máy bận.

Thời gian từng lớp cộng dồn (theo báo cáo Surefire) gần như không đổi: 1.544 s trước, 1.662 s sau. Lớp tích hợp chạy chậm hơn khi chia máy với các fork khác (`PlatformWorkflowIntegrationTest` 155 → 190 s), nhưng ba fork chạy cùng lúc. Lượt serial mất khoảng 3 phút.

## Các lượt lỗi trong lúc làm và cách xử lý

| Lượt | Lỗi | Nguyên nhân | Xử lý |
|---|---|---|---|
| 3 fork, lần 1 | `CrossUnitFlowIntegrationTest` lỗi cả lớp | Ba fork cùng bật RabbitMQ, cổng không mở kịp 60 s | Chờ khởi động 3 phút |
| 3 fork, lần 2 | Hai test perf chạy riêng trên JVM mới: U04_SCOPE 81 ms, fanout 7,5 s | JVM chưa nóng | Chạy cả lớp `RequirementsCoverageIntegrationTest` ở lượt serial |
| 3 fork, lần 3 | Lượt serial chạy 0 test; `JobDispatcherTest` lỗi | `excludedGroups` rỗng vẫn kế thừa `serial` của cấu hình chung; handler ném lỗi ngay vẫn quá 200 ms khi máy bận | `combine.self="override"`; tăng timeout handler giả |
| 3 fork, lần 4–5 | `DriveHealthIndicatorTest` | Lỗi đã ghi từ 26/11: test chờ probe 2 s, lần tạo mock đầu tiên mất khoảng chừng đó. Trong lượt serial, test này chạy đầu tiên trên JVM mới | Gắn tag `serial`; lượt serial chạy ngược thứ tự chữ cái để lớp lớn chạy trước. Logic test giữ nguyên theo yêu cầu trước đó |
| 3 fork, lần 6 | `DriveStorageIntegrationTest`: `End size 1 is less than fixed size 2` | Bản in MockMvc đọc header song song với luồng async | Tắt bản in ở hai lớp tải file async |

## Giới hạn

- Số đo trên một máy Windows; runner CI `ubuntu-latest` có 4 nhân, chưa đo ở đó. Nếu runner chậm, chỉnh bằng `-Dtest.forkCount=2`.
- `DriveHealthIndicatorTest` vẫn phụ thuộc thứ tự chạy trong lượt serial để JVM đã nóng. Cách sửa gốc là nới thời gian chờ trong test.
- Redis và RabbitMQ vẫn dùng container riêng cho từng lớp (7 lớp).
- Mỗi lớp vẫn tự chạy Flyway (52 migration, khoảng 5 s); chưa dùng database mẫu đã migrate.

Log và báo cáo của từng lượt nằm ở scratchpad của phiên, không đưa vào repo.
