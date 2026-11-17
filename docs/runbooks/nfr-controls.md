# Giới hạn tài nguyên và kiểm chứng NFR

## Health Drive

`/health` không chờ mạng Google Drive: indicator trả cache ngay, chỉ một probe nền được chạy tại một thời điểm. Cache 60 giây, probe quá 2 giây được báo `DEGRADED`; khi chưa có kết quả là `UNKNOWN`. Drive suy giảm không biến health tổng thành DOWN theo quyết định hiện hữu. Probe vẫn giữ slot tới khi I/O thực sự kết thúc nên không tạo thread mới liên tiếp khi provider không chịu ngắt.

Backend/worker vẫn có các health indicator database/Redis/RabbitMQ. Kiểm tra Drive chậm bằng `DriveHealthIndicatorTest`; không dùng việc health UP để khẳng định mọi dependency đang tốt.

## Database

Pool ứng dụng trong `application.yml` có tối đa 10 connection; chờ lấy connection tối đa 3 giây, kiểm connection 1 giây. pgJDBC connect timeout 5 giây, socket timeout 30 giây. Startup options đặt `statement_timeout=15000` và `lock_timeout=2000` (ms) cho mọi connection ứng dụng mới, gồm cả database đã tồn tại; không cần migration hay sửa role production.

Flyway dùng URL và credential migrator riêng, không thừa hưởng options của Hikari. `pg_dump` chạy qua process/credential riêng, không dùng pool này. SQL trong transaction backup vẫn chịu giới hạn 15 giây mỗi statement; thời gian chờ upload không bị giới hạn bởi statement timeout. Các unit có deadline ngắn hơn vẫn giữ nguyên.

Sau cập nhật phải tái tạo backend/worker để connection mới nhận cấu hình. Không thay giới hạn bằng `0` (vô hạn) để làm xanh benchmark. Nếu tác vụ hợp lệ cần deadline SQL dài hơn, phân tích truy vấn và kiểm lại trên dữ liệu đại diện trước khi cho ngoại lệ.

Test `DatabaseTimeoutIntegrationTest` tải cấu hình ứng dụng thật: giữ khóa bằng connection A, kiểm B bị hủy trong hạn, rollback không đổi dữ liệu; truy vấn chậm bị hủy và connection tiếp tục dùng được. Global error handler xử lý lỗi lock/query/transaction timeout thành `503 SYSTEM_BUSY`, kể cả JDBC trả SQLState `55P03`/`57014` trong `UncategorizedSQLException`; không lộ SQL. Handler riêng từng domain vẫn giữ contract lỗi hiện hữu.

## Worker

Executor tách theo queue, số handler tối đa đồng thời: triggered 2, email 1, AI 4, Drive 1, backup 1. Không có hàng đợi task không giới hạn trong process. Khi hết capacity, dispatcher ném lỗi hạ tầng để listener `nack/requeue` message gốc với backoff hiện có; không tăng attempt hoặc ghi FAILED cho công việc chưa được xử lý.

Timeout chỉ gửi interrupt; handler không chịu interrupt vẫn giữ slot và khóa idempotency tới khi thực sự kết thúc. Queue khác có executor riêng nên AI/Drive/backup bị kẹt không chiếm slot email. Khi shutdown, các executor được đóng.

## Image bất biến

`infra/images.lock.json` ghi digest manifest đọc từ Docker Hub ngày 16/11/2026; Dockerfiles, compose production/local/staging và script restore dùng cùng pin. App image phát hành tiếp tục dùng tag commit SHA theo quy trình hiện hữu. Local image build giữ tag `:local`.

Cập nhật image:

1. Chọn tag theo chính sách cập nhật của dự án, đọc digest index/manifest từ registry chính thức (không tự suy ra hash từ tên).
2. Đổi cả reference `tag@sha256:...` và bản ghi lock, giữ cùng PostgreSQL image ở compose/restore.
3. Chạy `node scripts/check-image-pins.mjs`, test/build/restore liên quan và ghi digest vào bằng chứng phát hành.
4. Review rồi phát hành theo quy trình hiện có. Pin digest không thay thế việc theo dõi lỗ hổng hoặc cập nhật bảo mật định kỳ.

CI frontend có bước kiểm pin. Pin khóa base/service image, không tuyên bố build byte-for-byte giống nhau khi các bước cài OS package vẫn truy cập repository bên ngoài.

## Tài liệu kỹ thuật tham chiếu

- [pgJDBC connection options và timeout](https://jdbc.postgresql.org/documentation/use/).
- [PostgreSQL 16: row lock FOR SHARE](https://www.postgresql.org/docs/16/explicit-locking.html).
- [Spring Boot 3.5: datasource/Hikari properties](https://docs.spring.io/spring-boot/3.5/appendix/application-properties/).
- [Khôi phục cô lập và cách ghi RPO/RTO](restore-isolated.md).

## Kiểm script restore bằng container thật

Trên Linux có Docker, sau khi build image backend cục bộ:

```sh
sh infra/scripts/test-restore-container.sh platform-backend:nfr-review
```

Script chỉ tạo fixture PostgreSQL riêng, dump dạng raw, gọi công cụ trong image backend rồi `pg_restore` vào container riêng; đối chiếu schema, JSON, điểm thập phân và số dòng. Container và anonymous volume được dọn sau kiểm tra. Cần đường dẫn thư mục tạm mà Docker daemon mount được (nếu chạy trong container điều khiển, mount cùng đường dẫn với host).

Đây là smoke test đường thực thi restore, không thay diễn tập archive dữ liệu thật, khóa mã hóa thật hoặc đo RPO/RTO toàn hệ thống. Các trường hợp archive mã hóa/sai khóa/bị sửa được kiểm ở `BackupEncryptorTest`.
